(ns ^:no-doc rewrite-clj.zip.whitespace
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.comment :as ncomment]
            [rewrite-clj.node.extras :as nextras]
            [rewrite-clj.node.whitespace :as nwhitespace]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Predicates

(defn whitespace?
  "Returns true when the current the node in `zloc` is a Clojure whitespace (which includes the comma)."
  [zloc]
  (some-> zloc zraw/node nwhitespace/whitespace?))

(defn linebreak?
  "Returns true when the current node in `zloc` is a linebreak."
  [zloc]
  (some-> zloc zraw/node nwhitespace/linebreak?))

(defn comment?
  "Returns true when the current node in `zloc` is a comment."
  [zloc]
  (some-> zloc zraw/node ncomment/comment?))

(defn whitespace-not-linebreak?
  "Returns true when current node in `zloc` is a whitespace but not a linebreak."
  [zloc]
  (and
   (whitespace? zloc)
   (not (linebreak? zloc))))

(defn whitespace-or-comment?
  "Returns true when current node in `zloc` is whitespace or a comment."
  [zloc]
  (some-> zloc zraw/node nextras/whitespace-or-comment?))

;; ## Movement

(defn skip
  "Return zipper with location moved to first location not satisfying predicate `p?` starting from the node in
   `zloc` and traversing by function `f`."
  [f p? zloc]
  (->> (iterate f zloc)
       (take-while identity)
       (take-while (complement zraw/end?))
       (drop-while p?)
       (first)))

(defn skip-whitespace
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc`
   and traversing by function `f`.

   `f` defaults to [[right]]"
  ([zloc] (skip-whitespace zraw/right zloc))
  ([f zloc] (skip f whitespace-or-comment? zloc)))

(defn skip-whitespace-left
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc` traversing left."
  [zloc]
  (skip-whitespace zraw/left zloc))

;; ## Insertion

(defn insert-space-left
  "Return zipper with `n` space whitespace node inserted to the left of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (insert-space-left zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (zraw/insert-left zloc (nwhitespace/spaces n))
     zloc)))

(defn insert-space-right
  "Return zipper with `n` space whitespace node inserted to the right of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (insert-space-right zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (zraw/insert-right zloc (nwhitespace/spaces n))
     zloc)))

(defn insert-newline-left
  "Return zipper with `n` newlines node inserted to the left of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (insert-newline-left zloc 1))
  ([zloc n]
   (zraw/insert-left zloc (nwhitespace/newlines n))))

(defn insert-newline-right
  "Return zipper with `n` newlines node inserted to the right of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (insert-newline-right zloc 1))
  ([zloc n]
   (zraw/insert-right zloc (nwhitespace/newlines n))))

;; ## Deprecated Functions

(defn prepend-space
   "DEPRECATED: renamed to [[insert-space-left]]."
  ([zloc n]
   (insert-space-left zloc (or n 1)))
  ([zloc]
   (prepend-space zloc nil)))

(defn append-space
   "DEPRECATED: renamed to [[insert-space-right]]."
  ([zloc n]
   (insert-space-right zloc (or n 1)))
  ([zloc]
   (append-space zloc nil)))

(defn prepend-newline
   "DEPRECATED: renamed to [[insert-newline-left]]."
  ([zloc n]
   (insert-newline-left zloc (or n 1)))
  ([zloc]
   (prepend-newline zloc nil)))

(defn append-newline
   "DEPRECATED: renamed to [[insert-newline-right]]."
  ([zloc n]
   (insert-newline-right zloc (or n 1)))
  ([zloc]
   (append-newline zloc nil)))
