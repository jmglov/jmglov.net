(ns lib
  (:require
   [clojure.string :as str]
   [highlighter :as h]
   [markdown.core :as md]))

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
