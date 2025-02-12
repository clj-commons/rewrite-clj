(ns ^:no-doc rewrite-clj.zip.insert
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.zip.whitespace :as zwhitespace]))

#?(:clj (set! *warn-on-reflection* true))

(def ^:private space
  (nwhitespace/spaces 1))

(defn- ends-with-ws? [zloc]
  (when zloc
    (or (zwhitespace/whitespace? zloc)
        ;; a comment is not necessarily newline terminated, but we don't special case that
        ;; inserting after a non-newline termnated comment is caveated in rewrite-clj.node/comment-node
        (zwhitespace/comment? zloc))))

(defn- starts-with-ws? [zloc]
  (zwhitespace/whitespace? zloc))

(defn insert-right
  "Return `zloc` with `item` inserted to the right of the current node in `zloc`, without moving location.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert spaces around `item` if necessary.
  Does not test `item` itself for whitespace.

  Use [[rewrite-clj.zip/insert-right*]] to insert without adding any whitespace."
  [zloc item]
  (let [next-zloc (zraw/right zloc)
        item-node (node/coerce item)]
    (cond-> zloc
      (and next-zloc (not (starts-with-ws? next-zloc)))
      (zraw/insert-right space)

      :always
      (zraw/insert-right item-node)

      (not (ends-with-ws? zloc))
      (zraw/insert-right space))))

(defn insert-left
  "Return zipper with `item` inserted to the left of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert spaces around `item` if necessary.
  Does not test `item` itself for whitespace.

  Use [[insert-left*]] to insert without adding any whitespace."
  [zloc item]
  (let [prev-zloc (zraw/left zloc)
        item-node (node/coerce item)]
    (cond-> zloc
      (and prev-zloc (not (ends-with-ws? prev-zloc)))
      (zraw/insert-left space)

      :always
      (zraw/insert-left item-node)

      (not (starts-with-ws? zloc))
      (zraw/insert-left space))))

(defn insert-child
  "Return `zloc` with `item` inserted as the first child of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert space after `item` if necessary.
  Does not test `item` itself for whitespace.

  Use [[insert-child*]] to insert without adding any whitespace."
  [zloc item]
  (let [prev-zloc (zraw/down zloc)
        item-node (node/coerce item)]
    (cond-> zloc
      (and prev-zloc (not (starts-with-ws? prev-zloc)))
      (zraw/insert-child space)

      :always
      (zraw/insert-child item-node))))

(defn append-child
  "Return `zloc` with `item` inserted as the last child of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert space before `item` if necessary.
  Does not test `item` itself for whitespace.

  Use [[append-child*]] to append without adding any whitespace."
  [zloc item]
  (let [prev-zloc (some-> zloc zraw/down zraw/rightmost)
        item-node (node/coerce item)]
    (cond-> zloc
      (and prev-zloc (not (ends-with-ws? prev-zloc)))
      (zraw/append-child space)

      :always
      (zraw/append-child item-node))))
