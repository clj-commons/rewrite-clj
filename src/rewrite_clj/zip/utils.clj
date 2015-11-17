(ns ^:no-doc rewrite-clj.zip.utils
  (:require [rewrite-clj.zip.zip :as z]))

;; ## Remove

(defn- update-in-path
  [{:keys [node path] :as loc} k f]
  (if-let [v (get loc k)]
    (assoc loc :changed? true k (f v))
    loc))

(defn remove-right
  "Remove right sibling of the current node (if there is one)."
  [loc]
  (update-in-path loc :right next))

(defn remove-left
  "Remove left sibling of the current node (if there is one)."
  [loc]
  (update-in-path loc :left pop))

(defn remove-right-while
  "Remove elements to the right of the current zipper location as long as
   the given predicate matches."
  [zloc p?]
  (loop [zloc zloc]
    (if-let [rloc (z/right zloc)]
      (if (p? rloc)
        (recur (remove-right zloc))
        zloc)
      zloc)))

(defn remove-left-while
  "Remove elements to the left of the current zipper location as long as
   the given predicate matches."
  [zloc p?]
  (loop [zloc zloc]
    (if-let [lloc (z/left zloc)]
      (if (p? lloc)
        (recur (remove-left zloc))
        zloc)
      zloc)))

;; ## Remove and Move

(defn remove-and-move-left
  "Remove current node and move left. If current node is at the leftmost
   location, returns `nil`."
  [{:keys [position left] :as loc}]
  (if (seq left)
    (assoc loc
           :changed? true
           :node (peek left)
           :left (pop left))))

(defn remove-and-move-right
  "Remove current node and move right. If current node is at the rightmost
   location, returns `nil`."
  [{:keys [position right] :as loc}]
  (if (seq right)
    (assoc loc
           :changed? true
           :node (first right)
           :right (next right))))
