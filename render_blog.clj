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

(def num-posts-in-index-page 3)

(def rendering-system-files
  [(fs/file ".." "templates")
   templates-dir])

(def cache-dir (fs/create-dirs (fs/file ".cache")))
(def out-dir (fs/create-dirs (fs/file "public" "blog")))

(def posts-file (fs/file blog-dir "posts.edn"))
(def posts (->> (lib/load-posts posts-dir default-metadata)
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
                    :cache-dir cache-dir
                    :out-dir out-dir
                    :post-template post-template
                    :posts-dir posts-dir
                    :rendering-system-files rendering-system-files}
                   post))

;;;; Generate archive page

(let [archive-file (fs/file out-dir "archive.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   archive-file)]
  (if (or rendering-modified? (lib/some-post-modified posts))
    (do
      (println "Writing archive page" (str archive-file))
      (spit archive-file
            (selmer/render page-template
                           {:skip-archive true
                            :title (str blog-title " - Archive")
                            :body (hiccup/html (lib/post-links {} "Archive" posts))})))
    (println "No posts modified; skipping archive file")))

;;;; Generate tag pages

(def posts-by-tag (lib/posts-by-tag posts))
(def tags-dir (fs/create-dirs (fs/file out-dir "tags")))

(let [tags-file (fs/file tags-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   tags-dir)]
  (if (or rendering-modified? (lib/some-post-modified posts))
    (do
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
                        tag-and-posts)))
    (println "No posts modified; skipping tag files")))

;;;; Generate index page with most recent posts

(let [index-file (fs/file out-dir "index.html")
      rendering-modified? (lib/rendering-modified? rendering-system-files
                                                   index-file)
      index-posts (take num-posts-in-index-page posts)
      new-index-posts? (lib/some-post-modified index-posts)]
  (if (or rendering-modified? new-index-posts?)
    (let [index (for [{:keys [metadata]} (take 3 posts)
                      :let [{:keys [file title date preview discuss]
                             :or {discuss discuss-fallback}}
                            metadata]
                      :when (not preview)]
                  [:div
                   [:h1 [:a {:href (str/replace file ".md" ".html")}
                         title]]
                   @(get @bodies file)
                   [:p "Discuss this post " [:a {:href discuss} "here"] "."]
                   [:p [:i "Published: " date]]])]
      (println "Writing index page" (str index-file))
      (spit index-file
            (selmer/render page-template
                           {:title blog-title
                            :body (hiccup/html {:escape-strings? false}
                                               index)})))
    (println "None of the" num-posts-in-index-page
             "most recent posts modified; skipping index page")))

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
            [:-cdata @(get @bodies file)]]])])
      xml/indent-str))

(let [feed-file (fs/file out-dir "atom.xml")
      clojure-feed-file (fs/file out-dir "planetclojure.xml")
      clojure-posts (filter
                     (fn [{:keys [metadata]}]
                       (some (:tags metadata) ["clojure" "clojurescript"]))
                     posts)]
  (if (or (lib/rendering-modified? rendering-system-files clojure-feed-file)
          (lib/some-post-modified clojure-posts))
    (do
      (println "Writing Clojure feed" (str clojure-feed-file))
      (spit clojure-feed-file
            (atom-feed clojure-posts)))
    (println "No Clojure posts modified; skipping Clojure feed"))
  (if (or (lib/rendering-modified? rendering-system-files feed-file)
          (lib/some-post-modified posts))
    (do
      (println "Writing feed" (str feed-file))
      (spit feed-file
            (atom-feed posts)))
    (println "No posts modified; skipping main feed")))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
