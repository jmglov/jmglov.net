(ns lib
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [highlighter :as h]
   [markdown.core :as md]
   [selmer.parser :as selmer]))

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

(defn html-file [file]
  (str/replace file ".md" ".html"))

(defn posts-by-tag [posts]
  (->> posts
       (sort-by :date)
       (mapcat (fn [{:keys [tags] :as post}]
                 (map (fn [tag] [tag post]) tags)))
       (reduce (fn [acc [tag post]]
                 (update acc tag #(conj % post)))
               {})))

(defn post-links [title posts]
  [:div {:style "width: 600px;"}
   [:h1 title]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str "../" (str/replace file ".md" ".html"))}
             title]
            " - "
            date]])]])

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
                           stale-check-files
                           work-dir]}
                   {:keys [file title date legacy discuss tags]
                    :or {discuss discuss-fallback}}]
  (let [cache-file (fs/file work-dir (html-file file))
        markdown-file (fs/file posts-dir file)
        stale-check-files (concat stale-check-files
                                  ["lib.clj" "highlighter.clj"])
        stale? (seq (fs/modified-since cache-file stale-check-files))
        body (if stale?
               (let [body (markdown->html markdown-file)]
                 (spit cache-file body)
                 body)
               (slurp cache-file))
        _ (swap! bodies assoc file body)
        body (selmer/render post-template {:body body
                                           :title title
                                           :date date
                                           :discuss discuss
                                           :tags tags})
        html (selmer/render base-html
                            {:title title
                             :body body})
        html-file (str/replace file ".md" ".html")]
    (spit (fs/file out-dir html-file) html)
    (let [legacy-dir (fs/file out-dir
                              (str/replace date "-" "/")
                              (str/replace file ".md" ""))]
      (when legacy
        (fs/create-dirs legacy-dir)
        (let [redirect-html (selmer/render"
<html><head>
<meta http-equiv=\"refresh\" content=\"0; URL=/{{new_url}}\" />
</head></html>"
                                          {:new_url html-file})]
          (spit (fs/file (fs/file legacy-dir "index.html")) redirect-html))))))

(defn write-tag! [{:keys [base-html
                          blog-title
                          tags-dir]}
                  [tag posts]]
  (let [tag-slug (str/replace tag #"[^A-z0-9]" "-")
        tag-file (fs/file tags-dir (str tag-slug ".html"))]
    (println "Writing tag page:" (.getName tag-file))
    (spit tag-file
          (selmer/render base-html
                         {:skip-archive true
                          :title (str blog-title " - Tag - " tag)
                          :relative-path "../"
                          :body (hiccup/html (post-links (str "Tag - " tag) posts))}))))
