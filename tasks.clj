(ns tasks
  (:require [babashka.cli]
            [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [quickblog.api :as qb]
            [tick.core :as t]))

(defn read-opts []
  (-> (slurp "opts.edn") edn/read-string))

(defn new-draft [opts args]
  (let [{:keys [file title date] :as parsed-opts}
        (babashka.cli/parse-opts (seq args)
                                 {:coerce {:tags []}})]
    (if (:help parsed-opts)
      (println "Usage: bb new-draft --file FILE --title TITLE [--date DATE]")
      (let [date (if (re-matches #"(?i)now|today" date) (str (t/date)) date)
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
                        :template-file "blog/new-post.md"}))))))
(defn render-blog [{:keys [favicon-dir out-dir] :as opts}]
  (fs/create-dirs out-dir)
  (doseq [path (fs/glob favicon-dir "**")
          :let [source (fs/file path)
                target (fs/file out-dir (fs/file-name source))]]
    (when (seq (fs/modified-since target source))
      (println "favicon modified; copying"
               (str source) "->" (str target))
      (fs/copy source target {:replace-existing true})))
  (qb/quickblog opts))

(defn set-date [opts args]
  (let [{:keys [file date] :as parsed-opts} (babashka.cli/parse-opts (seq args))]
    (if (:help parsed-opts)
      (println "Usage: bb set-date --file FILE --date DATE")
      (let [new-filename (str/replace file #"(?:draft|\d{4}-\d{2}-\d{2}-)(.+)$"
                                      (format "%s-$1" date))
            content (slurp file)
            assets (->> content
                        (re-seq #"assets/(?:draft|\d{4}-\d{2}-\d{2}-)([\S]+)")
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
