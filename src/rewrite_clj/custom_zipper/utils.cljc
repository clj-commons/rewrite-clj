(ns ^:no-doc rewrite-clj.custom-zipper.utils
  (:require [rewrite-clj.custom-zipper.core :as zraw]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Remove

(defn- update-in-path
  [[node path :as loc] k f]
  (let [v (get path k)]
    (if (seq v)
      (with-meta
        [node (assoc path k (f v) :changed? true)]
        (meta loc))
      loc)))

(defn remove-right
  "Remove right sibling of the current node (if there is one)."
  [loc]
  (if (zraw/custom-zipper? loc)
    (let [{[_r & rs] :right} loc]
      (assoc loc
             :right rs
             :changed? true))
    (update-in-path loc :r next)))

(defn remove-left
  "Remove left sibling of the current node (if there is one)."
  [loc]
  (if (zraw/custom-zipper? loc)
    (let [{:keys [left]} loc]
      (if-let [[_ lpos] (peek left)]
        (assoc loc
               :left (pop left)
               :position lpos
               :changed? true)
        loc))
    (update-in-path loc :l pop)))

(defn remove-right-while
  "Remove elements to the right of the current zipper location as long as
   the given predicate matches."
  [zloc p?]
  (loop [zloc zloc]
    (if-let [rloc (zraw/right zloc)]
      (if (p? rloc)
        (recur (remove-right zloc))
        zloc)
      zloc)))

(defn remove-left-while
  "Remove elements to the left of the current zipper location as long as
   the given predicate matches."
  [zloc p?]
  (loop [zloc zloc]
    (if-let [lloc (zraw/left zloc)]
      (if (p? lloc)
        (recur (remove-left zloc))
        zloc)
      zloc)))

;; ## Remove and Move

(defn remove-and-move-left
  "Remove current node and move left. If current node is at the leftmost
   location, returns `nil`."
  [loc]
  (if (zraw/custom-zipper? loc)
    (let [{:keys [left]} loc]
      (when (seq left)
        (let [[lnode lpos] (peek left)]
          (assoc loc
                 :changed? true
                 :node lnode
                 :position lpos
                 :left (pop left)))))
    (let [[_ {:keys [l] :as path}] loc]
      (when (seq l)
        (with-meta
          [(peek l) (-> path
                        (update-in [:l] pop)
                        (assoc :changed? true))]
          (meta loc))))))

(defn remove-and-move-right
  "Remove current node and move right. If current node is at the rightmost
   location, returns `nil`."
  [loc]
  (if (zraw/custom-zipper? loc)
    (let [{:keys [right]} loc]
      (when (seq right)
        (assoc loc
               :changed? true
               :node (first right)
               :right (next right))))

    (let [[_ {:keys [r] :as path}] loc]
      (when (seq r)
        (with-meta
          [(first r) (-> path
                         (update-in [:r] next)
                         (assoc :changed? true))]
          (meta loc))))))

(defn remove-and-move-up
  "Remove the current node and move up.
    `[a [b |c d]] -> [a |[b d]]`
    `[a [|b c d]] -> [a |[c d]]`"
  [loc]
  (if (zraw/custom-zipper? loc)
    (let [{:keys [left parent right]} loc]
      (if (nil? (:parent parent))
        (throw (ex-info "cannot remove at top" {}))
        (if (seq left)
          ;; same as zraw/up, except we don't include our current node
          (assoc parent
                 :changed? true
                 :node (zraw/make-node loc
                                       (:node parent)
                                       (concat (map first left) right)))
          (zraw/remove loc))))
    (let [[_node {l :l, ppath :ppath, pnodes :pnodes, rs :r, :as path}] loc]
      (if (nil? ppath)
        (throw (ex-info "cannot remove at top" {}))
        (if (pos? (count l))
          (zraw/up (with-meta [(peek l)
                               (assoc path :l (pop l) :changed? true)]
                     (meta loc)))
          (with-meta [(zraw/make-node loc (peek pnodes) rs)
                      (and ppath (assoc ppath :changed? true))]
            (meta loc)))))))
