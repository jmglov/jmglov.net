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

(def files (->> "files.edn"
                slurp
                (format "[%s]")
                edn/read-string
                (map (partial maria/mariafy maria-src-base-url))))

(def out-dir "public")

(def base-template "templates/base.html")

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(fs/copy-tree "assets" asset-dir {:replace-existing true})

(spit (fs/file out-dir "style.css")
      (slurp "templates/style.css"))

;;;; Sync favicon

(def favicon-dir (fs/create-dirs (fs/file out-dir)))

(fs/copy-tree "favicon" favicon-dir {:replace-existing true})

;;;; Generate posts from markdown

(defn html-file [file]
  (str/replace file ".md" ".html"))

(fs/create-dirs (fs/file ".work"))

(doseq [{:keys [file dir title context-map template]} files]
  (let [_ (when dir
            (fs/create-dirs (format ".work/%s" dir))
            (fs/create-dirs (format "%s/%s" out-dir dir)))
        cache-file (fs/file ".work" (html-file file))
        markdown-file (fs/file file)
        body (let [body (lib/markdown->html markdown-file)]
               (spit cache-file body)
               body)
        html (selmer/render (slurp (or template base-template))
                            (merge context-map {:title title
                                                :body body}))
        html-file (str/replace file ".md" ".html")]
    (spit (fs/file out-dir html-file) html)))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
