(ns ^{ :doc "Whitespace/Comment aware Zipper Modification."
       :author "Yannick Scherer" }
  rewrite-clj.zip.edit
  (:refer-clojure :exclude [replace remove])
  (:require [fast-zip.core :as z]
            [rewrite-clj.convert :as conv]
            [rewrite-clj.zip.core :as zc]))

;; ## Helpers

(def ^:private ^:const SPACE [:whitespace " "])

(defn- remove-trailing-space
  "Remove a single whitespace character after the given node."
  [zloc]
  (or
    (when-let [ws (z/right zloc)]
      (when (= (zc/tag ws) :whitespace)
        (let [w (dec (zc/length ws))]
          (-> ws
            (z/replace [:whitespace (apply str (repeat w \space))])
            z/left))))
    zloc))

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

(defn remove
  "Remove value at the given zipper location. Returns the first non-whitespace node 
   that would have preceded it in a depth-first walk. Will remove a single whitespace
   character following the current node and/or preceding it (if last element in branch)."
  [zloc]
  (->> zloc
    remove-trailing-space
    ;; TODO: Remove preceding space
    z/remove
    (zc/skip-whitespace z/prev)))

(defn splice
  "Add the current node's children to the parent branch (in place of the current node).
   The resulting zipper will be positioned on the first non-whitespace \"child\"."
  [zloc]
  (if-not (z/branch? zloc)
    zloc
    (let [ch (z/children zloc)]
      (-> (reduce z/insert-right zloc (reverse ch))
        z/remove
        z/next
        zc/skip-whitespace))))

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
