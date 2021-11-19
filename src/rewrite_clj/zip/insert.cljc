(ns ^:no-doc rewrite-clj.zip.insert
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.zip.whitespace :as zwhitespace]))

#?(:clj (set! *warn-on-reflection* true))

(def ^:private space
  (nwhitespace/spaces 1))

(defn- insert
  "Generic insertion helper. If the node reached by `move-fn`
   is a whitespace, insert an additional space."
  [move-fn insert-fn prefix zloc item]
  (let [item-node (node/coerce item)
        next-node (move-fn zloc)]
    (->> (concat
           (when (and next-node (not (zwhitespace/whitespace? next-node)))
             [space])
           [item-node]
           (when (not (zwhitespace/whitespace? zloc))
             prefix))
         (reduce insert-fn zloc))))

(defn insert-right
  "Return zipper with `item` inserted to the right of the current node in `zloc`, without moving location.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert a space if necessary.

  Use [[rewrite-clj.zip/insert-right*]] to insert without adding any whitespace."
  [zloc item]
  (insert
    zraw/right
    zraw/insert-right
    [space]
    zloc item))

(defn insert-left
  "Return zipper with `item` inserted to the left of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[insert-left*]] to insert without adding any whitespace."
  [zloc item]
  (insert
    zraw/left
    zraw/insert-left
    [space]
    zloc item))

(defn insert-child
  "Return zipper with `item` inserted as the first child of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[insert-child*]] to insert without adding any whitespace."
  [zloc item]
  (insert
    zraw/down
    zraw/insert-child
    []
    zloc item))

(defn append-child
  "Return zipper with `item` inserted as the last child of the current node in `zloc`, without moving.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[append-child*]] to append without adding any whitespace."
  [zloc item]
  (insert
    #(some-> % zraw/down zraw/rightmost)
    zraw/append-child
    []
    zloc item))
