(ns render
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [highlighter :as h]
   [markdown.core :as md]
   [selmer.parser :as selmer]))

(def files (edn/read-string (format "[%s]" (slurp "files.edn"))))

(def out-dir "public")

(def base-html
  (slurp "templates/base.html"))

;;;; Sync images and CSS

(def asset-dir (fs/create-dirs (fs/file out-dir "assets")))

(fs/copy-tree "assets" asset-dir {:replace-existing true})

(spit (fs/file out-dir "style.css")
      (slurp "templates/style.css"))

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

(doseq [{:keys [file title]} files]
  (let [cache-file (fs/file ".work" (html-file file))
        markdown-file (fs/file file)
        body (let [body (markdown->html markdown-file)]
               (spit cache-file body)
               body)
        html (selmer/render base-html
                            {:title title
                             :body body})
        html-file (str/replace file ".md" ".html")]
    (spit (fs/file out-dir html-file) html)))

;; for JVM Clojure:
(defn -main [& _args]
  (System/exit 0))
