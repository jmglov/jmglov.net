(ns maria
  (:require [babashka.fs :as fs]
            [clojure.string :as string]))

(def maria-base-url "https://dev.maria.cloud/http-text/")

(defn mariafy-file [src-url]
  (let [src-url (if-let [[_ raw-url] (re-matches #"^[^:]+://(.+)?" src-url)]
                  raw-url
                  src-url)]
    {:url (str maria-base-url src-url)
     :name (-> src-url (string/split #"/") last)}))

(defn mariafy [src-base-url {:keys [maria-dir] :as entry}]
  (if maria-dir
    (merge entry
           {:dir maria-dir
            :file (format "%s/%s" maria-dir "index.md")
            :context-map {:maria-files
                          (->> (fs/glob maria-dir "*.clj")
                               sort
                               (map #(mariafy-file (str src-base-url %))))}})
    entry))
