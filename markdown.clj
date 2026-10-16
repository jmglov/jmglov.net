(ns markdown
  (:require [clojure.string :as str]))

(defn pre-process-ref-links
  "Allow reference links without a blank line between the label and the reference
   definition; see: https://spec.commonmark.org/0.30/#shortcut-reference-link

   For example:

     ![Some alt text][ref]
     [ref]: image.png \"Some title\"

   should be the same as:

     ![Some alt text][ref]

     [ref]: image.png \"Some title\""
  [md]
  (str/replace md
               #"!?\[.+\]\[.+\]\n\[.+\]: .+(?:\n|$)"
               #(str/replace-first % "\n" "\n\n")))

(defn pre-process-img-refs
  "Handle image refs with attributes; e.g.
   
   ![Some alt text][imgref]
   [imgref]: image.png \"Some title\" border=1 width=800 class=\"fun\""
  [md]
  (str/replace md
               #"!\[.+\]\[.+\]\n\n\[.+\]:\s+\S+\s+\"([^\"]+)\"(\s.+)(?:\n|$)"
               (fn [[match title attrs]]
                 (str/replace match (format "\"%s\"%s" title attrs)
                              (format "\"%s{ATTRS:%s}\"" title
                                      (str/replace attrs "\"" "&quot;"))))))

(defn post-process-img-refs
  "Post processor for image refs with attributes."
  [html]
  (str/replace html #"title=\"([^{\"]+)[{]ATTRS:([^}]+)[}]\""
               (fn [[match title attrs]]
                 (format "title=\"%s\"%s"
                         title (str/replace attrs "&quot;" "\"")))))
