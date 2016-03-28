(ns ^:no-doc rewrite-clj.zip.whitespace
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.custom-zipper.core :as z]))

;; ## Predicates

(defn whitespace?
  [zloc]
  (some-> zloc z/node node/whitespace?))

(defn linebreak?
  [zloc]
  (some-> zloc z/node node/linebreak?))

(defn whitespace-or-comment?
  [zloc]
  (some-> zloc z/node node/whitespace-or-comment?))

;; ## Movement

(defn skip
  "Perform the given movement while the given predicate returns true."
  [f p? zloc]
  (->> (iterate f zloc)
       (take-while identity)
       (take-while (complement z/end?))
       (drop-while p?)
       (first)))

(defn skip-whitespace
  "Perform the given movement (default: `z/right`) until a non-whitespace/
   non-comment node is encountered."
  ([zloc] (skip-whitespace z/right zloc))
  ([f zloc] (skip f whitespace-or-comment? zloc)))

(defn skip-whitespace-left
  "Move left until a non-whitespace/non-comment node is encountered."
  [zloc]
  (skip-whitespace z/left zloc))

;; ## Insertion

(defn prepend-space
  "Prepend a whitespace node representing the given number of spaces (default: 1)."
  ([zloc] (prepend-space zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-left zloc (node/spaces n))
     zloc)))

(defn append-space
  "Append a whitespace node representing the given number of spaces (default: 1)."
  ([zloc] (append-space zloc 1))
  ([zloc n]
   {:pre [(>= n 0)]}
   (if (pos? n)
     (z/insert-right zloc (node/spaces n))
     zloc)))

(defn prepend-newline
  "Prepend a whitespace node representing the given number of spaces (default: 1)."
  ([zloc] (prepend-newline zloc 1))
  ([zloc n]
   (z/insert-left zloc (node/newlines n))))

(defn append-newline
  "Append a whitespace node representing the given number of spaces (default: 1)."
  ([zloc] (append-newline zloc 1))
  ([zloc n]
   (z/insert-right zloc (node/newlines n))))
