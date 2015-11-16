(ns ^:no-doc rewrite-clj.zip.utils
  (:require [rewrite-clj.zip.zip :as z]))

;; ## Remove

(defn- update-in-path
  [{:keys [node path position] :as loc} k f]
  (let [v (get path k)]
    (if (seq v)
      {:node node
       :path (assoc path k (f v) :changed? true)
       :position position}
      loc)))

(defn remove-right
  "Remove right sibling of the current node (if there is one)."
  [loc]
  (update-in-path loc :r next))

(defn remove-left
  "Remove left sibling of the current node (if there is one)."
  [loc]
  (update-in-path loc :l pop))

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
  [{:keys [position] {:keys [l] :as path} :path :as loc}]
  (if (seq l)
    {:node (peek l)
     :path (-> path
               (update-in [:l] pop)
               (assoc :changed? true))
     :position position}))

(defn remove-and-move-right
  "Remove current node and move right. If current node is at the rightmost
   location, returns `nil`."
  [{:keys [position] {:keys [r] :as path} :path :as loc}]
  (if (seq r)
    {:node (first r)
     :path (-> path
               (update-in [:r] next)
               (assoc :changed? true))
     :position position}))
