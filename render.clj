(ns render
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [lib]
   [maria]
   [selmer.parser :as selmer]))

(def maria-src-base-url "raw.githubusercontent.com/jmglov/jmglov.net/main/")

(def out-dir (fs/file "public"))

(def templates-dir (fs/file "templates"))
(def base-template (fs/file templates-dir "base.html"))

(def rendering-system-files
  ["lib.clj" templates-dir])

(def files (->> "files.edn"
                slurp
                (format "[%s]")
                edn/read-string
                (map (partial maria/mariafy maria-src-base-url))))

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(lib/copy-tree-modified (fs/file "assets")
                        asset-dir
                        (.getParent out-dir))

(let [style-src (fs/file templates-dir "style.css")
      style-target (fs/file out-dir "style.css")]
  (lib/copy-modified style-src style-target))

;;;; Sync favicon

(def favicon-dir (fs/create-dirs (fs/file out-dir)))

(doseq [file (fs/glob "favicon" "**")
        :let [outfile (fs/file favicon-dir (.getFileName file))]]
  (when (lib/stale? file outfile)
    (println "Writing favicon file:" (str outfile))))

;;;; Generate pages from markdown

(doseq [{:keys [file dir title context-map template]} files]
  (when dir
    (fs/create-dirs (format "%s/%s" out-dir dir)))
  (let [out-file (fs/file out-dir (lib/html-file file))
        markdown-file (fs/file file)]
    (when (or
           (lib/rendering-modified? rendering-system-files out-file)
           (lib/stale? markdown-file out-file))
      (let [body (lib/markdown->html markdown-file)]
        (println "Writing page:" (str out-file))
        (spit out-file
              (selmer/render (slurp (or template base-template))
                             (merge context-map {:title title
                                                 :body body})))))))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
