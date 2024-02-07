(ns user
  (:require [babashka.deps :as deps]
            [clojure.edn :as edn]
            [quickblog.api :as qb]
            [quickblog.internal :as lib]
            [tick.core :as t]))

(defn load-opts [base-opts]
  (-> base-opts
      #'qb/apply-default-opts
      lib/refresh-cache))

(comment

  (def opts (-> (slurp "opts.edn") edn/read-string load-opts))
  ;; => #'user/opts

  (->> opts
       :posts
       vals
       (take 1))
  ;; => ({:description
  ;;      "In which I make a bold statement, but then rather than explaining it or providing any evidence whatsoever, go on to talk about something completely different.",
  ;;      :tags #{"waffle"},
  ;;      :date "2022-08-26",
  ;;      :file "2022-08-26-doing-software-wrong.md",
  ;;      :title "We're doing software wrong",
  ;;      :next
  ;;      {:title "Do the most important thing first",
  ;;       :date "2022-08-27",
  ;;       :tags #{"waffle"},
  ;;       :description "In which I share the one true secret to Getting Thing Done™.",
  ;;       :image "assets/2022-08-27-preview.png",
  ;;       :image-alt
  ;;       "A man running down a gravel road in the countryside - Photo by Jenny Hill on Unsplash",
  ;;       :file "2022-08-27-most-important-first.md",
  ;;       :html #<Delay@4fa92433: :not-delivered>},
  ;;      :image-alt
  ;;      "A man on a mobile phone stands in front of a wall with the word \"productivity\" written on it - Photo by Andreas Klassen on Unsplash",
  ;;      :prev
  ;;      {:description
  ;;       "In which your intrepid blogger is introduced to scientific music and also makes excuses about not blogging.",
  ;;       :tags #{"diary"},
  ;;       :date "2022-08-25",
  ;;       :file "2022-08-25-scientific-music.md",
  ;;       :title "Scientific Music",
  ;;       :image-alt
  ;;       "Four musicians sit on the floor in traditional Indian dress, performing music",
  ;;       :image "assets/2022-08-25-preview.png",
  ;;       :html #<Delay@8361a35: :not-delivered>},
  ;;      :image "assets/2022-08-26-preview.jpg",
  ;;      :html #<Delay@1297ef5c: :not-delivered>})

  (->> opts
       :posts
       vals
       (remove #(= "FIXME" (:date %)))
       (map (comp t/date :date))
       (filter #(t/< % (t/date "2022-09-01")))
       count)
  ;; => 55

  )
