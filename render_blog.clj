(ns render-blog
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [lib]
   [selmer.parser :as selmer]))

(def blog-dir (fs/file "blog"))
(def out-dir (fs/file "public" "blog"))
(def templates-dir (fs/file blog-dir "templates"))
(def work-dir (fs/file ".work"))

(def stale-check-files
  (concat [*file*]
          (->> ["posts.edn"
                "templates"]
               (map (partial fs/file blog-dir)))))

(def blog-title "jmglov's blog")
(def discuss-fallback "https://github.com/jmglov/jmglov.net/discussions/categories/posts")

(def posts (->> (slurp (fs/file blog-dir "posts.edn"))
                (format "[%s]")
                edn/read-string
                (sort-by :date (comp - compare))))

(def base-html (slurp (fs/file templates-dir "base.html")))

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(fs/copy-tree (fs/file blog-dir "assets") asset-dir
              {:replace-existing true})

(spit (fs/file out-dir "style.css")
      (slurp (fs/file templates-dir "style.css")))

;;;; Generate posts from markdown

(def post-template (slurp (fs/file templates-dir "post.html")))

;; re-used when generating atom.xml
(def bodies (atom {}))

(fs/create-dirs work-dir)

(doseq [post posts]
  (lib/write-post! {:base-html base-html
                    :bodies bodies
                    :discuss-fallback discuss-fallback
                    :out-dir out-dir
                    :post-template post-template
                    :posts-dir (fs/file blog-dir "posts")
                    :stale-check-files stale-check-files
                    :work-dir ".work"}
                   post))

;;;; Generate archive page

(defn post-links []
  [:div {:style "width: 600px;"}
   [:h1 "Archive"]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str/replace file ".md" ".html")}
             title]
            " - "
            date]])]])

(spit (fs/file out-dir "archive.html")
      (selmer/render base-html
                     {:skip-archive true
                      :title (str blog-title " - Archive")
                      :body (hiccup/html (post-links))}))

;;;; Generate tag pages

(def posts-by-tag
  (->> posts
       (sort-by :date)
       (mapcat (fn [{:keys [tags] :as post}]
                 (map (fn [tag] [tag post]) tags)))
       (reduce (fn [acc [tag post]]
                 (update acc tag #(conj % post)))
               {})))

(defn tag-links [tag posts]
  [:div {:style "width: 600px;"}
   [:h1 (str "Tag - " tag)]
   [:ul.index
    (for [{:keys [file title date preview]} posts
          :when (not preview)]
      [:li [:span
            [:a {:href (str "../" (str/replace file ".md" ".html"))}
             title]
            " - "
            date]])]])

(def tags-dir (fs/create-dirs (fs/file out-dir "tags")))

(doseq [[tag posts] posts-by-tag
        :let [tag-slug (str/replace tag #"[^A-z0-9]" "-")
              tag-file (fs/file tags-dir (str tag-slug ".html"))]]
  (println "Writing tag file:" (.getName tag-file))
  (spit tag-file
        (selmer/render base-html
                       {:skip-archive true
                        :title (str blog-title " - Tag - " tag)
                        :relative-path "../"
                        :body (hiccup/html (tag-links tag posts))})))

;;;; Generate index page with last 3 posts

(defn index []
  (for [{:keys [file title date preview discuss]
         :or {discuss discuss-fallback}} (take 3 posts)
        :when (not preview)]
    [:div
     [:h1 [:a {:href (str/replace file ".md" ".html")}
           title]]
     (get @bodies file)
     [:p "Discuss this post " [:a {:href discuss} "here"] "."]
     [:p [:i "Published: " date]]]))

(spit (fs/file out-dir "index.html")
      (selmer/render base-html
                     {:title blog-title
                      :body (hiccup/html {:escape-strings? false} (index))}))

;;;; Generate atom feeds

(xml/alias-uri 'atom "http://www.w3.org/2005/Atom")
(import java.time.format.DateTimeFormatter)

(defn rfc-3339-now []
  (let [fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/now java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(defn rfc-3339 [yyyy-MM-dd]
  (let [in-fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd")
        local-date (java.time.LocalDate/parse yyyy-MM-dd in-fmt)
        fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/of (.atTime local-date 23 59 59) java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(def blog-root "http://jmglov.net/blog/")

(defn atom-feed
  ;; validate at https://validator.w3.org/feed/check.cgi
  [posts]
  (-> (xml/sexp-as-element
       [::atom/feed
        {:xmlns "http://www.w3.org/2005/Atom"}
        [::atom/title blog-title]
        [::atom/link {:href (str blog-root "atom.xml") :rel "self"}]
        [::atom/link {:href blog-root}]
        [::atom/updated (rfc-3339-now)]
        [::atom/id blog-root]
        [::atom/author
         [::atom/name "Josh Glover"]]
        (for [{:keys [title date file preview]} posts
              :when (not preview)
              :let [html (str/replace file ".md" ".html")
                    link (str blog-root html)]]
          [::atom/entry
           [::atom/id link]
           [::atom/link {:href link}]
           [::atom/title title]
           [::atom/updated (rfc-3339 date)]
           [::atom/content {:type "html"}
            [:-cdata (get @bodies file)]]])])
      xml/indent-str))

(spit (fs/file out-dir "atom.xml") (atom-feed posts))
(spit (fs/file out-dir "planetclojure.xml")
      (atom-feed (filter
                  (fn [post]
                    (some (:tags post) ["clojure" "clojurescript"]))
                  posts)))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
