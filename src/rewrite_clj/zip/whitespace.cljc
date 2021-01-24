(ns ^:no-doc rewrite-clj.zip.whitespace
  (:require [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Predicates

(defn whitespace?
  "Returns true when the current the node in `zloc` is a Clojure whitespace (which includes the comma)."
  [zloc]
  (some-> zloc z/node node/whitespace?))

(defn linebreak?
  "Returns true when the current node in `zloc` is a linebreak."
  [zloc]
  (some-> zloc z/node node/linebreak?))

(defn comment?
  "Returns true when the current node in `zloc` is a comment."
  [zloc]
  (some-> zloc z/node node/comment?))

(defn whitespace-not-linebreak?
  "Returns true when current node in `zloc` is a whitespace but not a linebreak."
  [zloc]
  (and
   (whitespace? zloc)
   (not (linebreak? zloc))))

(defn whitespace-or-comment?
  "Returns true when current node in `zloc` is whitespace or a comment."
  [zloc]
  (some-> zloc z/node node/whitespace-or-comment?))


;; ## Movement

(defn skip
  "Return zipper with location moved to first location not satisfying predicate `p?` starting from the node in
   `zloc` and traversing by function `f`."
  [f p? zloc]
  (->> (iterate f zloc)
       (take-while identity)
       (take-while (complement z/end?))
       (drop-while p?)
       (first)))

(defn skip-whitespace
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc`
   and traversing by function `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc] (skip-whitespace z/right zloc))
  ([f zloc] (skip f whitespace-or-comment? zloc)))

(defn skip-whitespace-left
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc` traversing left."
  [zloc]
  (skip-whitespace z/left zloc))

;; ## Insertion

(defn ^{:added "0.5.0"} insert-space-left
  "Return zipper with `n` space whitespace node inserted to the left of the current node in `zloc`.
   `n` defaults to 1."
  ([zloc] (insert-space-left zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-left zloc (node/spaces n))
     zloc)))

(defn ^{:added "0.5.0"} insert-space-right
  "Return zipper with `n` space whitespace node inserted to the right of the current node in `zloc`.
   `n` defaults to 1."
  ([zloc] (insert-space-right zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-right zloc (node/spaces n))
     zloc)))

(defn ^{:added "0.5.0"} insert-newline-left
  "Return zipper with `n` newlines node inserted to the left of the current node in `zloc`.
   `n` defaults to 1."
  ([zloc] (insert-newline-left zloc 1))
  ([zloc n]
   (z/insert-left zloc (node/newlines n))))

(defn ^{:added "0.5.0"} insert-newline-right
  "Return zipper with `n` newlines node inserted to the right of the current node in `zloc`.
   `n` defaults to 1."
  ([zloc] (insert-newline-right zloc 1))
  ([zloc n]
   (z/insert-right zloc (node/newlines n))))

;; ## Deprecated Functions

(defn ^{:deprecated "0.5.0"} prepend-space
   "DEPRECATED: renamed to [[insert-space-left]]."
  [zloc & [n]]
  (insert-space-left zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} append-space
   "DEPRECATED: renamed to [[insert-space-right]]."
  [zloc & [n]]
  (insert-space-right zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} prepend-newline
   "DEPRECATED: renamed to [[insert-newline-left]]."
  [zloc & [n]]
  (insert-newline-left zloc (or n 1)))

(defn ^{:deprecated "0.5.0"} append-newline
   "DEPRECATED: renamed to [[insert-newline-right]]."
  [zloc & [n]]
  (insert-newline-right zloc (or n 1)))
