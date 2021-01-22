(ns ^:no-doc rewrite-clj.zip.removez
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

(defn- remove-trailing-while
  "Remove all whitespace following a given node."
  [zloc p?]
  (u/remove-right-while zloc p?))

(defn- remove-preceding-while
  "Remove all whitespace preceding a given node."
  [zloc p?]
  (u/remove-left-while zloc p?))

(defn- remove-p
  [zloc p?]
  (->> (-> (if (or (m/rightmost? zloc)
                   (m/leftmost? zloc))
             (remove-preceding-while zloc p?)
             zloc)
           (remove-trailing-while p?)
           z/remove)
       (ws/skip-whitespace z/prev)))

(defn remove
  "Return zipper with current node in `zloc` removed. Returned zipper location
   is moved to the first non-whitespace node preceding removed node in a depth-first walk.
   Removes whitespace appropriately.

  - `[1 |2  3]    => [|1 3]`
  - `[1 |2]       => [|1]`
  - `[|1 2]       => |[2]`
  - `[|1]         => |[]`
  - `[  |1  ]     => |[]`
  - `[1 [2 3] |4] => [1 [2 |3]]`
  - `[|1 [2 3] 4] => |[[2 3] 4]`

   If the removed node is at the rightmost location, both preceding and trailing spaces are removed,
   otherwise only trailing spaces are removed. This means that a following element
   (no matter whether on the same line or not) will end up in the same position
   (line/column) as the removed one, _unless_ a comment lies between the original
   node and the neighbour."
  [zloc]
  {:pre [zloc]
   :post [%]}
  (remove-p zloc ws/whitespace?))

(defn remove-preserve-newline
  "Same as [[remove]] but preserves newlines."
  [zloc]
  {:pre [zloc]
   :post [%]}
  (remove-p zloc #(and (ws/whitespace? %) (not (ws/linebreak? %)))))
