(ns render-blog
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [lib]
   [selmer.parser :as selmer]))

(def blog-author "Josh Glover")
(def blog-title "jmglov's blog")
(def blog-root "http://jmglov.net/blog/")
(def discuss-fallback "https://github.com/jmglov/jmglov.net/discussions/categories/posts")

(def posts-file "posts.edn")

(def blog-dir (fs/file "blog"))
(def out-dir (fs/file "public" "blog"))
(def templates-dir (fs/file blog-dir "templates"))

(def rendering-system-files
  [(fs/file ".." "templates")
   templates-dir])

(def posts (->> (slurp (fs/file blog-dir posts-file))
                (format "[%s]")
                edn/read-string
                (sort-by :date (comp - compare))))

(def base-html (slurp (fs/file templates-dir "base.html")))

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(lib/copy-tree-modified (fs/file blog-dir "assets")
                        asset-dir
                        (.getParent out-dir))

(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (lib/copy-modified style-src style-target))

;;;; Generate posts from markdown

(def post-template (slurp (fs/file templates-dir "post.html")))
(def bodies (atom {}))  ; re-used when generating atom.xml

(doseq [post posts]
  (lib/write-post! {:base-html base-html
                    :bodies bodies
                    :discuss-fallback discuss-fallback
                    :out-dir out-dir
                    :post-template post-template
                    :posts-dir (fs/file blog-dir "posts")
                    :rendering-system-files rendering-system-files}
                   post))

;;;; Generate archive page

(let [archive-file (fs/file out-dir "archive.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   archive-file)
      new-posts? (lib/stale? posts-file archive-file)]
  (when (or rendering-modified? new-posts?)
    (println "Writing archive page" (str archive-file))
    (spit archive-file
          (selmer/render base-html
                         {:skip-archive true
                          :title (str blog-title " - Archive")
                          :body (hiccup/html (lib/post-links {} "Archive" posts))}))))

;;;; Generate tag pages

(def posts-by-tag (lib/posts-by-tag posts))
(def tags-dir (fs/create-dirs (fs/file out-dir "tags")))

(let [tags-file (fs/file tags-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   tags-file)
      new-posts? (lib/stale? posts-file tags-file)]
  (when (or rendering-modified? new-posts?)
    (println "Writing tags page" (str tags-file))
    (spit tags-file
          (selmer/render base-html
                         {:skip-archive true
                          :title (str blog-title " - Tags")
                          :relative-path "../"
                          :body (hiccup/html (lib/tag-links "Tags" posts-by-tag))}))
    (doseq [tag-and-posts posts-by-tag]
      (lib/write-tag! {:base-html base-html
                       :blog-title blog-title
                       :tags-dir tags-dir}
                      tag-and-posts))))

;;;; Generate index page with last 3 posts

(let [index-file (fs/file out-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   index-file)
      new-posts? (lib/stale? posts-file index-file)
      index (for [{:keys [file title date preview discuss]
                   :or {discuss discuss-fallback}} (take 3 posts)
                  :when (not preview)]
              [:div
               [:h1 [:a {:href (str/replace file ".md" ".html")}
                     title]]
               (get @bodies file)
               [:p "Discuss this post " [:a {:href discuss} "here"] "."]
               [:p [:i "Published: " date]]])]
  (when (or rendering-modified? new-posts?)
    (println "Writing index page" (str index-file))
    (spit index-file
          (selmer/render base-html
                         {:title blog-title
                          :body (hiccup/html {:escape-strings? false}
                                             index)}))))

;;;; Generate atom feeds

(xml/alias-uri 'atom "http://www.w3.org/2005/Atom")

(defn atom-feed
  ;; validate at https://validator.w3.org/feed/check.cgi
  [posts]
  (-> (xml/sexp-as-element
       [::atom/feed
        {:xmlns "http://www.w3.org/2005/Atom"}
        [::atom/title blog-title]
        [::atom/link {:href (str blog-root "atom.xml") :rel "self"}]
        [::atom/link {:href blog-root}]
        [::atom/updated (lib/rfc-3339-now)]
        [::atom/id blog-root]
        [::atom/author
         [::atom/name blog-author]]
        (for [{:keys [title date file preview]} posts
              :when (not preview)
              :let [html (str/replace file ".md" ".html")
                    link (str blog-root html)]]
          [::atom/entry
           [::atom/id link]
           [::atom/link {:href link}]
           [::atom/title title]
           [::atom/updated (lib/rfc-3339 date)]
           [::atom/content {:type "html"}
            [:-cdata (get @bodies file)]]])])
      xml/indent-str))

(let [feed-file (fs/file out-dir "atom.xml")
      clojure-feed-file (fs/file out-dir "planetclojure.xml")
      rendering-modified? (or
                           (lib/rendering-modified? rendering-system-files
                                                    feed-file)
                           (lib/rendering-modified? rendering-system-files
                                                    clojure-feed-file))
      new-posts? (or (lib/stale? posts-file feed-file)
                     (lib/stale? posts-file clojure-feed-file))]
  (when (or rendering-modified? new-posts?)
    (println "Writing feed" (str feed-file))
    (spit feed-file (atom-feed posts))
    (println "Writing Clojure feed" (str clojure-feed-file))
    (spit clojure-feed-file
          (atom-feed (filter
                      (fn [post]
                        (some (:tags post) ["clojure" "clojurescript"]))
                      posts)))))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
