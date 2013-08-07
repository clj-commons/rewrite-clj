(ns ^{ :doc "Utility Operations for Zippers"
       :author "Yannick Scherer" }
  rewrite-clj.zip.utils
  (:require [fast-zip.core :as z])
  (:import [fast_zip.core ZipperPath ZipperLocation]))

(defn remove-right
  "Remove right sibling of the current node (if there is one)."
  [^ZipperLocation zloc]
  (let [path ^ZipperPath (.path zloc)]
    (if (zero? (count (.r path)))
      zloc
      (ZipperLocation.
        (.branch? zloc)
        (.children zloc)
        (.make-node zloc)
        (.node zloc)
        (assoc path :r (clojure.core/next (.r path)) :changed? true)))))

(defn remove-left
  "Remove left sibling of the current node (if there is one)."
  [^ZipperLocation zloc]
  (let [path ^ZipperPath (.path zloc)]
    (if (zero? (count (.l path)))
      zloc
      (ZipperLocation.
        (.branch? zloc)
        (.children zloc)
        (.make-node zloc)
        (.node zloc)
        (assoc path :l (pop (.l path)) :changed? true)))))

(defn remove-and-move-left
  "Remove current node and move left. If current node is at the leftmost
   location, returns `nil`."
  [^ZipperLocation zloc]
  (let [path ^ZipperPath (.path zloc)]
    (when (pos? (count (.l path)))
      (ZipperLocation.
        (.branch? zloc)
        (.children zloc)
        (.make-node zloc)
        (peek (.l path))
        (assoc path :l (pop (.l path)) :changed? true)))))

(defn remove-and-move-right
  "Remove current node and move right. If current node is at the rightmost
   location, returns `nil`."
  [^ZipperLocation zloc]
  (let [path ^ZipperPath (.path zloc)]
    (when (pos? (count (.r path)))
      (ZipperLocation.
        (.branch? zloc)
        (.children zloc)
        (.make-node zloc)
        (first (.r path))
        (assoc path :r (clojure.core/next (.r path)) :changed? true)))))
