(ns ^{ :doc "Operations for Clojure Data Structures."
       :author "Yannick Scherer" }
  rewrite-clj.zip.seqs
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set? replace])
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip.core :refer [tag value]]
            [rewrite-clj.zip.move :refer [up down left right]]
            [rewrite-clj.zip.edit :refer [append-child replace]]
            [rewrite-clj.zip.find :refer [find-value]]))

(defn seq?
  [zloc]
  (contains? #{:forms :list :vector :set :map} (tag zloc)))

(defn list?
  [zloc]
  (= (tag zloc) :list))

(defn vector?
  [zloc]
  (= (tag zloc) :vector))

(defn set?
  [zloc]
  (= (tag zloc) :set))

(defn map?
  [zloc]
  (= (tag zloc) :map))

(defn map
  "Apply function to all zipper locations in the seq at the current zipper location.
   If this is called on a map, the map values be traversed."
 [f zloc]
  (when-not (seq? zloc)
    (throw (Exception. (str "cannot iterate over node of type: " (tag zloc)))))
  (if (map? zloc)
    (loop [loc (down zloc)
           parent zloc]
      (if-not (and loc (z/node loc))
        parent
        (if-let [v0 (right loc)]
          (if-let [v (f v0)]
            (recur (right v) (up v))
            (recur (right v0) parent))
          parent)))
    (loop [loc (down zloc)
           parent zloc]
      (if-not (and loc (z/node loc))
        parent
        (if-let [v (f loc)]
          (recur (right v) (up v))
          (recur (right loc) parent))))))

(defn map-keys
  "Apply function to all keys of the map at the current zipper location"
  [f zloc]
  (when-not (map? zloc)
    (throw (Exception. (str "cannot iterate over keys of node of type: " (tag zloc)))))
  (loop [loc (down zloc)
         parent zloc]
    (if-not (and loc (z/node loc))
      parent
      (if-let [v (f loc)]
        (recur (right (right v)) (up v))
        (recur (right (right loc)) parent)))))

(defn get
  "If a map is given, get element with the given key; if a seq is given, get nth element."
  [zloc k]
  (cond (map? zloc) (when-let [v (-> zloc down (find-value k))]
                      (right v))
        (seq? zloc) (let [elements (->> zloc down (iterate right) (take-while identity))]
                      (nth elements k))
        :else (throw (Exception. (str "cannot get element of node of type: " (tag zloc))))))

(defn assoc
  "Set map/seq element to the given value."
  [zloc k v]
  (if-let [vloc (get zloc k)]
    (-> vloc (replace v) up)
    (if (map? zloc)
      (-> zloc (append-child k) (append-child v))
      (throw (Exception. (str "not a valid index to assoc: " v))))))
