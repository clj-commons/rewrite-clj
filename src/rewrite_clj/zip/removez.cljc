(ns ^:no-doc rewrite-clj.zip.removez
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

(defn- node-depth
  "Return current node location depth in `zloc`, top is 0."
  [zloc]
  (->> (iterate zraw/up zloc)
       (take-while identity)
       count
       dec))

(defn- has-trailing-linebreak-at-eoi?
  "Returns true when current node is last node in zipper and trailing whitespace contains
  at least 1 newline."
  [zloc]
  (and (= 1 (node-depth zloc))
       (not (m/right zloc))
       (->> (iterate zraw/right zloc)
            (take-while identity)
            (some ws/linebreak?))))

(defn- left-ws-trim
  ([zloc]
   (left-ws-trim zloc ws/whitespace?))
  ([zloc p?]
   (if (or (m/rightmost? zloc)
           (m/leftmost? zloc))
     (u/remove-left-while zloc p?)
     zloc)))

(defn- right-ws-trim
  ([zloc]
   (right-ws-trim zloc ws/whitespace?))
  ([zloc p?]
   (u/remove-right-while zloc p?)))

(defn- right-ws-trim-keep-trailing-linebreak [zloc]
  (let [right-trimmed (right-ws-trim zloc)]
    (if (has-trailing-linebreak-at-eoi? zloc)
      (ws/insert-newline-right right-trimmed)
      right-trimmed)))

(defn- remove-with-trim
  [zloc left-ws-trim-fn right-ws-trim-fn]
  (->> zloc
       left-ws-trim-fn
       right-ws-trim-fn
       zraw/remove
       (ws/skip-whitespace zraw/prev)))

(defn remove
  "Return `zloc` with current node removed. Returned zipper location
   is moved to the first non-whitespace node preceding removed node in a depth-first walk.
   Removes whitespace appropriately.

  - `[1 |2  3]    => [|1 3]`
  - `[1 |2]       => [|1]`
  - `[|1 2]       => |[2]`
  - `[|1]         => |[]`
  - `[  |1  ]     => |[]`
  - `[1 [2 3] |4] => [1 [2 |3]]`
  - `[|1 [2 3] 4] => |[[2 3] 4]`

   If the removed node is a rightmost sibling, both leading and trailing whitespace
   is removed, otherwise only trailing whitespace is removed.

   The result is that a following element (no matter whether it is on the same line
   or not) will end up at same positon (line/column) as the removed one.
   If a comment lies betwen the original node and the neighbour this will not hold true.

   If the removed node is at end of input and is trailed by 1 or more newlines,
   a single trailing newline will be preserved.

   Use [[remove*]] to remove node without removing any surrounding whitespace."
  [zloc]
  {:pre [zloc]
   :post [%]}
  (remove-with-trim zloc
                    left-ws-trim
                    right-ws-trim-keep-trailing-linebreak))

(defn remove-preserve-newline
  "Same as [[remove]] but preserves newlines.
   Specifically: will trim all whitespace - or whitespace up to first linebreak if present."
  [zloc]
  {:pre [zloc]
   :post [%]}
  (let [ws-pred-fn #(and (ws/whitespace? %) (not (ws/linebreak? %)))]
    (remove-with-trim zloc
                      #(left-ws-trim % ws-pred-fn)
                      #(right-ws-trim % ws-pred-fn))))
