(ns lib
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
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
                                  [*file* "highlighter.clj"])
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
