;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/zip/remove.clj
(ns ^:no-doc rewrite-clj.zip.remove
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.zip.removez]))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.removez
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

   Use [[rewrite-clj.zip/remove*]] to remove node without removing any surrounding whitespace."
  [zloc] (rewrite-clj.zip.removez/remove zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.removez
(defn remove-preserve-newline
  "Same as [[remove]] but preserves newlines.
   Specifically: will trim all whitespace - or whitespace up to first linebreak if present."
  [zloc] (rewrite-clj.zip.removez/remove-preserve-newline zloc))
