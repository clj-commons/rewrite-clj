(ns ^{ :doc "Base Operations for Zipper" 
       :author "Yannick Scherer" }
  rewrite-clj.zip.core
  (:require [fast-zip.core :as z]
            [rewrite-clj.convert :as conv]))

;; ## Zipper

(defn- z-branch?
  [node]
  (when (clojure.core/vector? node)
    (let [[k & _] node]
      (contains? #{:forms :list :vector :set :map :meta :meta* :reader-macro} k))))

(defn- z-make-node
  [node ch]
  (apply vector (first node) ch))

(def edn
  "Create zipper over rewrite-clj's EDN tree structure."
  (partial z/zipper z-branch? rest z-make-node))

;; ## Convert

(defn sexpr
  "Get the s-expression of the current node."
  [zloc]
  (when zloc
    (conv/->sexpr (z/node zloc))))

;; ## Access

(defn tag
  "Get tag of structure at current zipper location."
  [zloc]
  (when zloc
    (first (z/node zloc))))

(defn value
  "Get the first child of the current zipper node."
  [zloc]
  (when zloc
    (second (z/node zloc))))

(defn whitespace?
  "Check if the node at the current zipper location is whitespace or comment."
  [zloc]
  (contains? #{:comment :whitespace :newline} (tag zloc)))

(defn linebreak?
  "Check if the node at the current zipper location is a linebreak."
  [zloc]
  (= (tag zloc) :newline))

;; ## Skip

(defn skip
  "Skip locations that match the given predicate by applying the given movement function
   to the initial zipper location."
  [f p? zloc]
  (->> zloc
    (iterate f)
    (take-while identity)
    (take-while (complement z/end?))
    (drop-while p?)
    (first)))

(defn skip-whitespace
  "Apply movement function (default: `clojure.z/right`) until a non-whitespace/non-comment
   element is reached."
  ([zloc] (skip-whitespace z/right zloc))
  ([f zloc] (skip f whitespace? zloc)))

(defn skip-whitespace-left
  "Move left until a non-whitespace/non-comment element is reached."
  [zloc]
  (skip-whitespace z/left zloc))

;; ## Whitespace Nodes

(defn prepend-space
  "Prepend a whitespace node of the given width."
  ([zloc] (prepend-space zloc 1))
  ([zloc n] (z/insert-left zloc [:whitespace (apply str (repeat n \space))])))

(defn append-space
  "Append a whitespace node of the given width."
  ([zloc] (append-space zloc 1))
  ([zloc n] (z/insert-right zloc [:whitespace (apply str (repeat n \space))])))

(defn prepend-newline
  "Prepend a whitespace node with the given number of linebreaks."
  ([zloc] (prepend-newline zloc 1))
  ([zloc n] (z/insert-left zloc [:newline (apply str (repeat n \newline))])))

(defn append-newline
  "Prepend a whitespace node with the given number of linebreaks."
  ([zloc] (append-newline zloc 1))
  ([zloc n] (z/insert-right zloc [:newline (apply str (repeat n \newline))])))

;; ## Edit Scope

(defn subzip
  "Create zipper that only contains the given zipper location's node."
  [zloc]
  (when zloc
    (edn (z/node zloc))))

(defmacro edit->
  "Will pass arguments to `->`. Return value will be the state of the input node
   after all modifications have been performed. This means that the result is 
   automatically 'zipped up' to represent the same location the macro was given."
  [zloc & body]
  `(let [zloc# ~zloc
         loc# (subzip zloc#)]
     (if-let [edit# (-> loc# ~@body)]
       (z/replace zloc# (z/root edit#))
       (throw (Exception. "Body of edit-> did not return value.")))))
