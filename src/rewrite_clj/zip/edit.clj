(ns ^{ :doc "Whitespace/Comment aware Zipper Modification."
       :author "Yannick Scherer" }
  rewrite-clj.zip.edit
  (:refer-clojure :exclude [replace remove])
  (:require [fast-zip.core :as z]
            [rewrite-clj.convert :as conv]
            [rewrite-clj.zip.core :as zc]
            [rewrite-clj.zip.utils :as zu]))

;; ## Helpers

(def ^:private ^:const SPACE [:whitespace " "])

;; ## Insert

(defn insert-right
  "Insert item to the right of the current location. Will insert a space if necessary."
  [zloc item]
  (let [r (-> zloc z/right)
        item (conv/->tree item)]
    (cond (not (z/node zloc)) (-> zloc (z/replace item))
          (or (not r) (zc/whitespace? r)) (-> zloc (z/insert-right item) (z/insert-right SPACE))
          :else (-> zloc (z/insert-right SPACE) (z/insert-right item) (z/insert-right SPACE)))))

(defn insert-left
  "Insert item to the left of the current location. Will insert a space if necessary."
  [zloc item]
  (let [r (-> zloc z/left)
        item (conv/->tree item)]
    (cond (not (z/node zloc)) (-> zloc (z/replace item))
          (or (not r) (zc/whitespace? r)) (-> zloc (z/insert-left item) (z/insert-left SPACE))
          :else (-> zloc (z/insert-left SPACE) (z/insert-left item) (z/insert-left SPACE)))))

(defn insert-child
  "Insert item as child of the current location. Will insert a space if necessary."
  [zloc item]
  (let [r (-> zloc z/down)
        item (conv/->tree item)]
    (if (or (not r) (not (z/node r)) (zc/whitespace? r))
      (-> zloc (z/insert-child item))
      (-> zloc (z/insert-child SPACE) (z/insert-child item)))))

(defn append-child
  "Append item as child of the current location. Will insert a space if necessary."
  [zloc item]
  (let [r (-> zloc z/down z/rightmost)
        item (conv/->tree item)]
    (if (or (not r) (not (z/node r)) (zc/whitespace? r))
      (-> zloc (z/append-child item))
      (-> zloc (z/append-child SPACE) (z/append-child item)))))

;; ## Modify

(defn replace
  "Replace value at the given zipper location with the given value."
  [zloc v]
  (z/replace zloc (conv/->tree v)))

(defn edit
  "Replace value at the given zipper location with value of (f node-sexpr args)."
  [zloc f & args]
  (let [form (zc/sexpr zloc)]
    (z/replace zloc (conv/->tree (apply f form args)))))

;; ## Remove

(defn- remove-trailing-space
  "Remove all whitespace following a given node."
  [zloc]
  (loop [zloc zloc]
    (if-let [rloc (z/right zloc)]
      (if (zc/whitespace? rloc)
        (recur (zu/remove-right zloc))
        zloc)
      zloc)))

(defn- remove-preceding-space
  [zloc]
  (loop [zloc zloc]
    (if-let [lloc (z/left zloc)]
      (if (zc/whitespace? lloc)
        (recur (zu/remove-left zloc))
        zloc)
      zloc)))

(defn remove
  "Remove value at the given zipper location. Returns the first non-whitespace node 
   that would have preceded it in a depth-first walk. Will remove whitespace appropriately.
  
     [1  2  3]   => [1  3]
     [1 2]       => [1]
     [1 2]       => [2]
     [1]         => []
     [  1  ]     => []
     [1 [2 3] 4] => [1 [2 3]]
     [1 [2 3] 4] => [[2 3] 4]

  If a node is located rightmost, both preceding and trailing spaces are removed, otherwise only
  trailing spaces are touched. This means that a following element (no matter whether on the
  same line or not) will end up in the same position (line/column) as the removed one."
  [zloc]
  (let [zloc (if (zc/rightmost? zloc) (remove-preceding-space zloc) zloc)
        zloc (-> zloc remove-trailing-space z/remove)]
    (zc/skip-whitespace z/prev zloc)))

;; ## Splice

(defn splice
  "Add the current node's children to the parent branch (in place of the current node).
   The resulting zipper will be positioned on the first non-whitespace \"child\". If 
   the node does not have children (or only whitespace children), `nil` is returned."
  [zloc]
  (when (and (z/branch? zloc) (pos? (count (z/children zloc))) (zc/skip-whitespace (z/down zloc)))
    (->> (reverse (z/children zloc))
      (reduce z/insert-right zloc)
      zu/remove-and-move-right
      zc/skip-whitespace)))

(defn splice-or-remove
  "Try to splice the given node. If it fails (no children, only whitespace children),
   remove it."
  [zloc]
  (or (splice zloc) (remove zloc)))

;; ## Prefix

(defmulti prefix
  "Prefix the value of a zipper node with the given string. This supports multi-lined strings."
  (fn [zloc p]
    (when zloc
      (first (z/node zloc))))
  :default nil)

(defmethod prefix nil
  [zloc prefix]
  (throw (Exception. (str "Cannot prefix value of type: " (first (z/node zloc))))))

(defmethod prefix :token
  [zloc prefix]
  (let [v (second (z/node zloc))
        v1 (cond (string? v) (str prefix v)
                 (symbol? v) (symbol (namespace v) (str prefix (name v)))
                 (keyword? v) (keyword (namespace v) (str prefix (name v)))
                 :else (throw (Exception. (str "Cannot prefix token: " v))))]
    (z/replace zloc [:token v1])))

(defmethod prefix :multi-line
  [zloc prefix]
  (let [[v & rst] (rest (z/node zloc))]
    (z/replace zloc (vec (list* :multi-line (str prefix v) rst)))))

;; ## Suffix

(defmulti suffix
  "Suffix the value of a zipper node with the given string. This supports multi-lined strings."
  (fn [zloc p]
    (when zloc
      (first (z/node zloc))))
  :default nil)

(defmethod suffix nil
  [zloc suffix]
  (throw (Exception. (str "Cannot suffix value of type: " (first (z/node zloc))))))

(defmethod suffix :token
  [zloc suffix]
  (let [v (second (z/node zloc))
        v1 (cond (string? v) (str v suffix)
                 (symbol? v) (symbol (namespace v) (str (name v) suffix))
                 (keyword? v) (keyword (namespace v) (str (name v) suffix))
                 :else (throw (Exception. (str "Cannot suffix token: " v))))]
    (z/replace zloc [:token v1])))

(defmethod suffix :multi-line
  [zloc suffix]
  (let [[v & rst] (rest (z/node zloc))]
    (z/replace zloc (vec (list* :multi-line (str v suffix) rst)))))
