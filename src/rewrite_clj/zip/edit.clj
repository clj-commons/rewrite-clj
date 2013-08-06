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
