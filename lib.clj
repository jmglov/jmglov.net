(ns lib
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [highlighter :as h]
   [markdown.core :as md]
   [selmer.parser :as selmer])
  (:import (java.time.format DateTimeFormatter)))

(defn rendering-modified? [rendering-system-files target-file]
  (seq (fs/modified-since target-file rendering-system-files)))

(defn stale? [src target]
  (seq (fs/modified-since target src)))

(defn copy-modified [src target]
  (when (stale? src target)
    (println "Writing" (str target))
    (fs/create-dirs (.getParent (fs/file target)))
    (fs/copy src target)))

(defn copy-tree-modified [src-dir target-dir out-dir]
  (let [modified-paths (fs/modified-since (fs/file target-dir)
                                          (fs/file src-dir))
        new-paths (->> (fs/glob src-dir "**")
                       (remove #(fs/exists? (fs/file out-dir %))))]
    (doseq [path (concat modified-paths new-paths)
            :let [target-path (fs/file out-dir path)]]
      (fs/create-dirs (.getParent target-path))
      (println "Writing" (str target-path))
      (fs/copy (fs/file path) target-path))))

(defn rfc-3339-now []
  (let [fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/now java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(defn rfc-3339 [yyyy-MM-dd]
  (let [in-fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd")
        local-date (java.time.LocalDate/parse yyyy-MM-dd in-fmt)
        fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/of (.atTime local-date 23 59 59)
                                        java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(defn html-file [file]
  (str/replace file ".md" ".html"))

(defn markdown->html [file]
  (let [_ (println "Processing markdown for file:" (str file))
        markdown (slurp file)
        markdown (h/highlight-clojure markdown)
        ;; make links without markup clickable
        markdown (str/replace markdown #"http[A-Za-z0-9/:.=#?_-]+([\s])"
                              (fn [[match ws]]
                                (format "[%s](%s)%s"
                                        (str/trim match)
                                        (str/trim match)
                                        ws)))
        ;; allow links with markup over multiple lines
        markdown (str/replace markdown #"\[[^\]]+\n"
                              (fn [match]
                                (str/replace match "\n" "$$RET$$")))
        html (md/md-to-html-string markdown :reference-links? true)
        html (str/replace html "$$RET$$" "\n")]
    html))

(defn posts-by-tag [posts]
  (->> posts
       (sort-by :date)
       (mapcat (fn [{:keys [tags] :as post}]
                 (map (fn [tag] [tag post]) tags)))
       (reduce (fn [acc [tag post]]
                 (update acc tag #(conj % post)))
               {})))

(defn post-links [{:keys [relative-path]} title posts]
  [:div {:style "width: 600px;"}
   [:h1 title]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str relative-path (str/replace file ".md" ".html"))}
             title]
            " - "
            date]])]])

(defn render-page [{:keys [outfile sharing-template] :as config}
                   template template-vars]
  (let [sharing-metadata (when sharing-template
                           (selmer/render (slurp sharing-template)
                                          template-vars))]
    (selmer/render template
                   (merge template-vars
                          {:sharing-metadata sharing-metadata}))))

(defn tag-links [title tags]
  [:div {:style "width: 600px;"}
   [:h1 title]
   [:ul.index
    (for [[tag posts] tags]
      [:li [:span
            [:a {:href (str tag ".html")} tag]
            " - "
            (count posts)
            " posts"]])]])

(defn write-post! [{:keys [base-html
                           bodies
                           discuss-fallback
                           out-dir
                           post-template
                           posts-dir
                           rendering-system-files]
                    :as config}
                   {:keys [file title date discuss tags]
                    :or {discuss discuss-fallback}}]
  (let [out-file (fs/file out-dir (html-file file))
        markdown-file (fs/file posts-dir file)
        rendering-system-files (concat rendering-system-files
                                       ["lib.clj" "highlighter.clj"])
        ;; We need to build the body regardless of whether the file is
        ;; stale because it's used in the index page and RSS feed. This
        ;; really needs to be cleaned up.
        body (markdown->html markdown-file)
        _ (swap! bodies assoc file body)]
    (when (or (rendering-modified? rendering-system-files out-file)
              (stale? markdown-file out-file))
      (let [body (selmer/render post-template {:body body
                                               :title title
                                               :date date
                                               :discuss discuss
                                               :tags tags})
            html (render-page config base-html
                              {:title title
                               :body body})]
        (println "Writing post:" (str out-file))
        (spit out-file html)
        (let [legacy-dir (fs/file out-dir
                                  (str/replace date "-" "/")
                                  (str/replace file ".md" ""))])))))

(defn write-tag! [{:keys [base-html
                          blog-title
                          tags-dir]
                   :as config}
                  [tag posts]]
  (let [tag-slug (str/replace tag #"[^A-z0-9]" "-")
        tag-file (fs/file tags-dir (str tag-slug ".html"))]
    (println "Writing tag page:" (str tag-file))
    (spit tag-file
          (render-page config base-html
                       {:skip-archive true
                        :title (str blog-title " - Tag - " tag)
                        :relative-path "../"
                        :body (hiccup/html (lib/post-links {:relative-path "../"}
                                                           (str "Tag - " tag) posts))}))))
