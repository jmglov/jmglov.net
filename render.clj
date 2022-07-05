(ns render
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [highlighter :as h]
   [maria]
   [markdown.core :as md]
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

(defn markdown->html [file]
  (let [_ (println "Processing markdown for file:" (str file))
        markdown (slurp file)
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
        html (md/md-to-html-string markdown)
        html (str/replace html "$$RET$$" "\n")]
    html))

(defn html-file [file]
  (str/replace file ".md" ".html"))

(fs/create-dirs (fs/file ".work"))

(doseq [{:keys [file dir title context-map template]} files]
  (let [_ (when dir
            (fs/create-dirs (format ".work/%s" dir))
            (fs/create-dirs (format "%s/%s" out-dir dir)))
        cache-file (fs/file ".work" (html-file file))
        markdown-file (fs/file file)
        body (let [body (markdown->html markdown-file)]
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
