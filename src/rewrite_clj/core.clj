(ns
  rewrite-clj.core
  (:require [clojure.zip :as zip]))

;; ## Zipper

(defn- z-branch?
  [node]
  (when (vector? node)
    (let [[k & _] node]
      (contains? #{:list :vector :set :map} k))))

(defn- z-children
  [node]
  (second node))

(defn- z-make-node
  [node ch]
  (let [[k & _] node]
    (vector node (seq ch))))

(def edn-zip 
  "Create zipper over rewrite-clj's EDN tree structure."
  (partial zip/zipper z-branch? z-children z-make-node))

;; ## Access

(defn tag
  "Get tag of structure at current zipper location."
  [zloc]
  (first (zip/node zloc)))

;; ## Skip

(defn skip
  "Skip locations that match the given predicate by applying the given movement function
   to the initial zipper location."
  [f p? zloc]
  (->> zloc
    (iterate f)
    (take-while identity)
    (drop-while p?)
    (first)))

(defn skip-whitespace
  "Apply movement function (default: `clojure.zip/right`) until a non-whitespace/non-comment
   element is reached."
  ([zloc] (skip-whitespace zip/right zloc))
  ([f zloc] (skip f #(contains? #{:whitespace :comment} (tag %)) zloc)))

(defn skip-whitespace-left
  "Move left until a non-whitespace/non-comment element is reached."
  [zloc]
  (skip-whitespace zip/left zloc))

;; ## Find

(defn- find-loc
  "Find element satisfying the given predicate by applying the given movement function
   to the initial zipper location."
  [f p? zloc]
  (->> zloc
    (iterate f)
    (take-while identity)
    (drop-while (complement p?))
    (first)))

(defn find-right
  "Find element satisfying the given predicate by moving right."
  [p? zloc]
  (find-loc zip/right p? zloc))

(defn find-left
  "Find element satisfying the given predicate by moving left."
  [p? zloc]
  (find-loc zip/left p? zloc))

(defn find-by-tag
  "Find element with the given tag by applying the given movement function to the initial
   zipper location, defaulting to `clojure.zip/right`."
  ([t zloc] (find-by-tag zip/right t zloc))
  ([f t zloc] (find-loc f #(= (tag %) t) zloc)))

(defn find-left-by-tag
  "Find element with the given tag by moving left."
  [t zloc]
  (find-by-tag zip/left t zloc))

(defn find-next-by-tag
  "Find next element with the given tag, moving to the right before beginning the search."
  [t zloc]
  (when-let [zloc (zip/right zloc)]
    (find-by-tag zip/right t zloc)))

(defn find-previous-by-tag
  "Find previous element with the given tag, moving to the left before beginning the search."
  [t zloc]
  (when-let [zloc (zip/left zloc)]
    (find-by-tag zip/left t zloc)))
