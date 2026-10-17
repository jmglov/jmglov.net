(ns tasks
  (:require [babashka.cli]
            [babashka.fs :as fs]
            [babashka.http-server :as http]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [quickblog.api :as qb]
            [selmer.parser :as selmer]
            [tick.core :as t]
            [maria]
            [markdown :as md]
            [lib]))

(defn render-list [opts xs]
  (cond
    (empty? opts) xs
    (string? xs) (render-list opts [xs])
    :else (map #(selmer/render % opts) xs)))

(defn ->file
  ([parts]
   (->file {} parts))
  ([opts parts]
   (let [parts (if (empty? opts)
                 parts
                 (render-list opts parts))]
     (apply fs/file parts))))

(defn render-site-opts [opts]
  (let [opts (->> [:site-templates-dir
                   :site-base-template]
                  (map (fn [k]
                         [k (->file opts (get opts k))]))
                  (into {})
                  (merge opts))]
    (-> opts
        (update :site-rendering-system-files #(render-list opts %)))))

(defn read-opts []
  (-> (slurp "opts.edn")
      edn/read-string
      render-site-opts
      (assoc
       :pre-process-markdown-fns [md/pre-process-ref-links
                                  md/pre-process-img-refs]
       :post-process-markdown-fns [md/post-process-img-refs])))

(defn render-site [{:keys [assets-dir
                           templates-dir
                           site-out-dir
                           site-templates-dir
                           site-base-template
                           site-rendering-system-files
                           maria-src-base-url]
                    :as opts}]
  (let [files (->> "files.edn"
                   slurp
                   (format "[%s]")
                   edn/read-string
                   (map (partial maria/mariafy maria-src-base-url)))
        assets-out-dir (fs/create-dirs (fs/file site-out-dir assets-dir))
        favicon-dir (fs/create-dirs site-out-dir)
        style-src (fs/file templates-dir "style.css")
        style-target (fs/file site-out-dir "style.css")]
    (when (fs/exists? assets-dir)
      (lib/copy-tree-modified assets-dir assets-out-dir))
    (lib/copy-modified style-src style-target)
    (doseq [file (fs/glob "favicon" "**")
            :let [outfile (fs/file favicon-dir (fs/file-name file))]]
      (when (lib/stale? file outfile)
        (println "Writing favicon file:" (str outfile))))
    (doseq [{:keys [file dir title context-map]} files]
      (when dir
        (fs/create-dirs (format "%s/%s" site-out-dir dir)))
      (let [out-file (fs/file site-out-dir (lib/html-file file))
            markdown-file (fs/file file)]
        (when (or
               (lib/rendering-modified? site-rendering-system-files
                                        out-file)
               (lib/stale? markdown-file out-file)
               (lib/stale? site-base-template out-file))
          (let [body (lib/markdown->html opts markdown-file)]
            (println "Writing page:" (str out-file))
            (spit out-file
                  (selmer/render (slurp site-base-template)
                                 (merge opts
                                        context-map
                                        {:title title
                                         :body body})))))))))

(defn new-draft [opts args]
  (let [{:keys [file title date] :as parsed-opts}
        (babashka.cli/parse-opts (seq args)
                                 {:coerce {:tags []}})
        default-template "blog/new-post.md"]
    (if (:help parsed-opts)
      (->> ["Usage: bb new-draft --file FILE [--title TITLE] [--date DATE] ..."
            ""
            "Arguments:"
            "  --file           filename; will be prefixed with DATE if present or 'draft'"
            "  --title          [default: FILE]"
            "  --date           [default: 'FIXME'] date; use 'now' or 'today' for the current date"
            "  --image          [default: FILE-preview.png] preview image (social sharing)"
            "  --preview        [default: true] make post a preview"
            (format "  --template-file  [default: %s] template to use for new post"
                    default-template)]
           (str/join "\n")
           println)
      (let [date (if (and date (re-matches #"(?i)now|today" date))
                   (str (t/date))
                   date)
            file' (format "%s-%s" (or date "draft") file)]
        (qb/new (merge opts
                       parsed-opts
                       {:file file'
                        :title (or title
                                   (-> file
                                       (str/replace "-" " ")
                                       str/capitalize))
                        :date (or date "FIXME")
                        :image (format "%s-preview.png"
                                       (str/replace file' (re-pattern "[.]md$") ""))
                        :preview true
                        :template-file default-template}))))))
(defn render-blog [{:keys [favicon-dir out-dir] :as opts}]
  (fs/create-dirs out-dir)
  (doseq [path (fs/glob favicon-dir "**")
          :let [source (fs/file path)
                target (fs/file out-dir (fs/file-name source))]]
    (when (seq (fs/modified-since target source))
      (println "favicon modified; copying"
               (str source) "->" (str target))
      (fs/copy source target {:replace-existing true})))
  (doseq [path (fs/glob out-dir "*.html")
          :let [f (fs/file path)]]
    (when (-> (slurp f) (str/includes? "livejs"))
      (println "Killing livejs file:" (str (fs/file-name f)))
      (fs/delete f)))
  (qb/quickblog opts))

(defn http-server [{:keys [http-port site-out-dir] :as opts}]
  (let [http-port (if (int? http-port) http-port (Integer/parseInt http-port))]
    (println (format "Starting webserver on port %d with root %s"
                     http-port site-out-dir))
    (try
      (http/serve {:port http-port, :dir site-out-dir})
      ;; Annoyingly, this prints even when an exception is thrown
      (println (format "Serving static assets at http://localhost:%d" http-port))
      (catch java.net.BindException e
        (println (format "Address in use: localhost:%d" http-port))
        (throw e)))))

(defn set-date [opts args]
  (let [{:keys [file date] :as parsed-opts} (babashka.cli/parse-opts (seq args))
        date (if (re-matches #"(?i)now|today" date) (str (t/date)) date)]
    (if (:help parsed-opts)
      (println "Usage: bb set-date --file FILE --date DATE")
      (let [new-filename (str/replace file #"(?:draft-|\d{4}-\d{2}-\d{2}-)(.+)$"
                                      (format "%s-$1" date))
            content (slurp file)
            assets (->> content
                        (re-seq #"assets/(?:draft-|\d{4}-\d{2}-\d{2}-)([\S]+)")
                        set)
            content' (-> (reduce (fn [acc [filename asset-name]]
                                   (str/replace acc filename
                                                (format "assets/%s-%s" date asset-name)))
                                 content
                                 assets)
                         (str/replace #"Date: \S+\n" (format "Date: %s\n" date)))]
        (println "Writing" new-filename)
        (spit new-filename content')
        (doseq [[filename asset-name] assets]
          (let [source (format "blog/%s" filename)
                target (format "blog/assets/%s-%s" date asset-name)]
            (println "Renaming" source "->" target)
            (fs/move source target)))
        (println "Removing" file)
        (fs/delete file)))))
