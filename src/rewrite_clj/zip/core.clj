(ns ^{ :doc "Base Operations for Zipper"
       :author "Yannick Scherer" }
  rewrite-clj.zip.core
  (:require [fast-zip.core :as z]
            [rewrite-clj.convert :as conv]
            [rewrite-clj.printer :as prn]))

;; ## Zipper

(declare skip-whitespace)

(defn- z-branch?
  [node]
  (when (clojure.core/vector? node)
    (let [[k & _] node]
      (contains? #{:forms :list :vector :set :map :meta :meta* :reader-macro} k))))

(defn- z-make-node
  [node ch]
  (apply vector (first node) ch))

(def edn* (partial z/zipper z-branch? rest z-make-node))

(def edn
  "Create zipper over rewrite-clj's EDN tree structure. The returned zipper location will
   be the first non-whitespace node _inside_ a top-level `:forms` node. If the given tree
   is not a `:forms` node, it will be wrapped in one."
  (fn [tree]
    (skip-whitespace
      (z/down
        (edn*
          (if (= (first tree) :forms)
            tree
            (vector :forms tree)))))))

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

(defn length
  "Get the length of the current node if it is represented as a string."
  [zloc]
  (when zloc
    (prn/estimate-length (z/node zloc))))

(defn comment?
  "Check if the node at the current zipper location is a comment."
  [zloc]
  (= (tag zloc) :comment))

(defn whitespace?
  "Check if the node at the current zipper location is whitespace (including linebreak)."
  [zloc]
  (contains? #{:whitespace :newline} (tag zloc)))

(defn whitespace-or-comment?
  "Check if the node at the current zipper location is whitespace (including linebreak)
   or comment."
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
  ([f zloc] (skip f whitespace-or-comment? zloc)))

(defn skip-whitespace-left
  "Move left until a non-whitespace/non-comment element is reached."
  [zloc]
  (skip-whitespace z/left zloc))

(defn leftmost?
  "Check if the given zipper is at the leftmost non-whitespace position."
  [zloc]
  (nil? (skip-whitespace-left (z/left zloc))))

(defn rightmost?
  "Check if the given zipper is at the rightmost non-whitespace position."
  [zloc]
  (nil? (skip-whitespace (z/right zloc))))

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
    (edn* (z/node zloc))))

(defn move-to-node
  [zloc dest-loc]
  (let [path (->>
               (iterate z/up dest-loc)
               (take-while z/up)
               (map (comp count z/lefts))
               (reverse))]
    (reduce
      (fn [loc index]
        (nth (iterate z/right (z/down loc)) index))
      (edn* (z/root zloc))
      path)))

(defn edit-node
  "Apply given function to the given zipper location. Afterwards move the resulting
   zipper to the same location as the original one."
  [zloc f]
  (if-let [eloc (f zloc)]
    (move-to-node eloc zloc)
    (throw (Exception. "Function supplied to edit-node did not return value!"))))

(defn edit-children
  "Apply given function to the given zipper location. It may only alter the node itself or
   its children. Afterwards, move the resulting zipper to the same location as the original one."
  [zloc f]
  (if-not (z/branch? zloc)
    (throw (Exception. "Zipper location supplied to edit-children is not a branch!"))
    (if-let [eloc (f (subzip zloc))]
      (z/replace zloc (z/root eloc))
      (throw (Exception. "Function supplied to edit-children did not return value!")))))

(defmacro edit->
  "Will pass arguments to `->`. Return value will be the state of the input node
   after all modifications have been performed. This means that the result is
   automatically 'zipped up' to represent the same location the macro was given.
   This only works correctly if insertions/removals are limited to nodes below or right
   of the input node (modification can occur anywhere)."
  [zloc & body]
  `(let [zloc# ~zloc]
     (edit-node zloc# (fn [zloc#] (-> zloc# ~@body)))))

(defmacro edit->>
  "Will pass arguments to `->>`. Return value will be the state of the input node
   after all modifications have been performed. This means that the result is
   automatically 'zipped up' to represent the same location the macro was given.
   This only works correctly if insertions/removals are limited to nodes below or right
   of the input node (modification can occur anywhere)."
  [zloc & body]
  `(let [zloc# ~zloc]
     (edit-node zloc# (fn [zloc#] (->> zloc# ~@body)))))

(defmacro subedit->
  "Will pass arguments to `->`. Return value will be the state of the input node after
   all modifications have been performed. This means that the result is automatically
   'zipped up' to represent the same location the macro was given. This only allows
   for modifications of the node itself or its children."
  [zloc & body]
  `(let [zloc# ~zloc]
     (edit-children zloc# (fn [zloc#] (-> zloc# ~@body)))))

(defmacro subedit->>
  "Will pass arguments to `->>`. Return value will be the state of the input node after
   all modifications have been performed. This means that the result is automatically
   'zipped up' to represent the same location the macro was given. This only allows
   for modifications of the node itself or its children."
  [zloc & body]
  `(let [zloc# ~zloc]
     (edit-children zloc# (fn [zloc#] (->> zloc# ~@body)))))
