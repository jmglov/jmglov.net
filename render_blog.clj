(ns render-blog
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [lib]
   [selmer.parser :as selmer]))

(def default-metadata
  {:author "Josh Glover"
   :copyright "cc/by-nc/4.0"
   :tags #{"waffle"}})

(def blog-author "Josh Glover")
(def blog-title "jmglov's blog")
(def blog-root "http://jmglov.net/blog/")
(def discuss-fallback "https://github.com/jmglov/jmglov.net/discussions/categories/posts")

(def blog-dir (fs/file "blog"))
(def posts-dir (fs/file blog-dir "posts"))
(def templates-dir (fs/file blog-dir "templates"))

(def page-template (slurp (fs/file templates-dir "base.html")))
(def post-template (slurp (fs/file templates-dir "post.html")))

(def rendering-system-files
  [(fs/file ".." "templates")
   templates-dir])

(def out-dir (fs/file "public" "blog"))

(def posts-file (fs/file blog-dir "posts.edn"))
(def posts (->> (lib/load-posts-from-dir posts-dir default-metadata)
                (lib/add-modified-metadata posts-dir out-dir)))

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(lib/copy-tree-modified (fs/file blog-dir "assets")
                        asset-dir
                        (.getParent out-dir))

(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (lib/copy-modified style-src style-target))

;;;; Generate posts from markdown

(def bodies (atom {}))  ; re-used when generating atom.xml

(doseq [post posts]
  (lib/write-post! {:page-template page-template
                    :bodies bodies
                    :discuss-fallback discuss-fallback
                    :out-dir out-dir
                    :post-template post-template
                    :posts-dir posts-dir
                    :rendering-system-files rendering-system-files}
                   post))

;;;; Generate archive page

(let [archive-file (fs/file out-dir "archive.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   archive-file)
      new-posts? (lib/stale? posts-file archive-file)]
  (when true #_(or rendering-modified? new-posts?)
    (println "Writing archive page" (str archive-file))
    (spit archive-file
          (selmer/render page-template
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
  (when true #_(or rendering-modified? new-posts?)
    (println "Writing tags page" (str tags-file))
    (spit tags-file
          (selmer/render page-template
                         {:skip-archive true
                          :title (str blog-title " - Tags")
                          :relative-path "../"
                          :body (hiccup/html (lib/tag-links "Tags" posts-by-tag))}))
    (doseq [tag-and-posts posts-by-tag]
      (lib/write-tag! {:page-template page-template
                       :blog-title blog-title
                       :tags-dir tags-dir}
                      tag-and-posts))))

;;;; Generate index page with last 3 posts

(let [index-file (fs/file out-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   index-file)
      new-posts? (lib/stale? posts-file index-file)
      index (for [{:keys [metadata]} (take 3 posts)
                  :let [{:keys [file title date preview discuss]
                         :or {discuss discuss-fallback}}
                        metadata]
                  :when (not preview)]
              [:div
               [:h1 [:a {:href (str/replace file ".md" ".html")}
                     title]]
               (get @bodies file)
               [:p "Discuss this post " [:a {:href discuss} "here"] "."]
               [:p [:i "Published: " date]]])]
  (when true #_(or rendering-modified? new-posts?)
    (println "Writing index page" (str index-file))
    (spit index-file
          (selmer/render page-template
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
        (for [{:keys [metadata]} posts
              :when (not (:preview metadata))
              :let [{:keys [title date file preview]} metadata
                    html (str/replace file ".md" ".html")
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
  (when true #_(or rendering-modified? new-posts?)
    (println "Writing feed" (str feed-file))
    (spit feed-file (atom-feed posts))
    (println "Writing Clojure feed" (str clojure-feed-file))
    (spit clojure-feed-file
          (atom-feed (filter
                      (fn [{:keys [metadata]}]
                        (some (:tags metadata) ["clojure" "clojurescript"]))
                      posts)))))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
