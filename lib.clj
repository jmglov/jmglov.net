(ns lib
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [highlighter :as h]
   [markdown.core :as md]
   [selmer.parser :as selmer])
  (:import (java.time.format DateTimeFormatter)))

(def metadata-transformers
  {:default first
   :tags #(-> % first (str/split #",\s*") set)})

(def required-metadata
  #{:date
    :title})

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
        {:keys [html]} (md/md-to-html-string-with-meta markdown :reference-links? true)
        html (str/replace html "$$RET$$" "\n")]
    html))

(defn transform-metadata
  ([metadata]
   (transform-metadata metadata {}))
  ([metadata default-metadata]
   (->> metadata
        (map (fn [[k v]]
               (let [transformer (or (metadata-transformers k)
                                     (metadata-transformers :default))]
                 [k (transformer v)])))
        (into {})
        (merge default-metadata))))

(defn markdown->html-meta
  ([file]
   (markdown->html-meta file {}))
  ([file default-metadata]
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
                                 (str/replace match "\n" "$$RET$$")))]
     (-> (md/md-to-html-string-with-meta markdown :reference-links? true)
         (update :metadata #(transform-metadata % default-metadata))
         (assoc-in [:metadata :file] (.getName file))
         (update :html #(str/replace % "$$RET$$" "\n"))))))

(defn load-posts
  "Returns posts in descending date order"
  [posts-file]
  (->> (slurp posts-file)
       (format "[%s]")
       edn/read-string
       (sort-by :date (comp - compare))))

(defn load-posts-from-dir
  "Returns all posts from `post-dir` in descending date order"
  ([posts-dir]
   (load-posts-from-dir posts-dir {}))
  ([posts-dir default-metadata]
   (->> (fs/glob posts-dir "*.md")
        (map #(markdown->html-meta (.toFile %) default-metadata))
        (remove
         (fn [{:keys [metadata]}]
           (when-let [missing-keys
                      (seq (set/difference required-metadata
                                           (set (keys metadata))))]
             (println "Skipping" (:file metadata)
                      "due to missing required metadata:"
                      (str/join ", " (map name missing-keys)))
             :skipping)))
        (sort-by (comp :date :metadata) (comp - compare)))))

(defn add-modified-metadata
  "Adds :modified? to each post showing if it is new or modified more recently than `out-dir`"
  [posts-dir out-dir posts]
  (let [post-files (map #(fs/file posts-dir (get-in % [:metadata :file])) posts)
        html-file-exists? #(->> (get-in % [:metadata :file])
                                fs/file
                                .getName
                                lib/html-file
                                (fs/file out-dir)
                                fs/exists?)
        new-posts (->> (remove html-file-exists? posts)
                       (map (comp :file :metadata))
                       set)
        modified-posts (->> post-files
                            (fs/modified-since out-dir)
                            (map #(str (.getFileName %)))
                            set)
        new-or-modified-posts (set/union new-posts modified-posts)]
    (map #(assoc-in %
                    [:metadata :modified?]
                    (contains? new-or-modified-posts (get-in % [:metadata :file])))
         posts)))

(defn posts-by-tag [posts]
  (->> posts
       (sort-by (comp :date :metadata))
       (mapcat (fn [{:keys [metadata] :as post}]
                 (map (fn [tag] [tag post]) (:tags metadata))))
       (reduce (fn [acc [tag post]]
                 (update acc tag #(conj % post)))
               {})))

(defn post-links [{:keys [relative-path]} title posts]
  [:div {:style "width: 600px;"}
   [:h1 title]
   [:ul.index
    (for [{:keys [metadata]} posts
          :let [{:keys [file title date preview]} metadata]
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

(defn write-post! [{:keys [bodies
                           discuss-fallback
                           out-dir
                           page-template
                           post-template
                           posts-dir
                           rendering-system-files]
                    :as config}
                   {:keys [metadata html]}]
  (let [{:keys [file title date discuss tags]
         :or {discuss discuss-fallback}} metadata
        out-file (fs/file out-dir (html-file file))
        markdown-file (fs/file posts-dir file)
        rendering-system-files (concat rendering-system-files
                                       ["lib.clj" "highlighter.clj"])
        ;; We need to build the body regardless of whether the file is
        ;; stale because it's used in the index page and RSS feed. This
        ;; really needs to be cleaned up.
        body html
        _ (swap! bodies assoc file body)]
    (when (or (rendering-modified? rendering-system-files out-file)
              (stale? markdown-file out-file))
      (let [body (selmer/render post-template {:body body
                                               :title title
                                               :date date
                                               :discuss discuss
                                               :tags tags})
            rendered-html (render-page config page-template
                                       {:title title
                                        :body body})]
        (println "Writing post:" (str out-file))
        (spit out-file rendered-html)
        (let [legacy-dir (fs/file out-dir
                                  (str/replace date "-" "/")
                                  (str/replace file ".md" ""))])))))

(defn write-tag! [{:keys [page-template
                          blog-title
                          tags-dir]
                   :as config}
                  [tag posts]]
  (let [tag-slug (str/replace tag #"[^A-z0-9]" "-")
        tag-file (fs/file tags-dir (str tag-slug ".html"))]
    (println "Writing tag page:" (str tag-file))
    (spit tag-file
          (render-page config page-template
                       {:skip-archive true
                        :title (str blog-title " - Tag - " tag)
                        :relative-path "../"
                        :body (hiccup/html (lib/post-links {:relative-path "../"}
                                                           (str "Tag - " tag) posts))}))))
