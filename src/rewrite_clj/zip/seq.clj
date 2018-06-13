(ns ^:no-doc rewrite-clj.zip.seq
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.node.predicates :as pred]
            [rewrite-clj.zip
             [edit :as e]
             [find :as f]
             [insert :as i]
             [move :as m]]
            [rewrite-clj.custom-zipper.core :as z]))

;; ## Predicates

(defn seq?
  [zloc]
  (pred/seq? (z/node zloc)))

(defn list?
  [zloc]
  (pred/list? (z/node zloc)))

(defn vector?
  [zloc]
  (pred/vector? (z/node zloc)))

(defn set?
  [zloc]
  (pred/set? (z/node zloc)))

(defn map?
  [zloc]
  (pred/map? (z/node zloc)))

;; ## Map Operations

(defn- map-seq
  [f zloc]
  {:pre [(seq? zloc)]}
  (if-let [n0 (m/down zloc)]
    (some->> (f n0)
             (iterate
               (fn [loc]
                 (if-let [n (m/right loc)]
                   (f n))))
             (take-while identity)
             (last)
             (m/up))
    zloc))

(defn map-vals
  "Apply function to all value nodes of the given map node."
  [f zloc]
  {:pre [(map? zloc)]}
  (loop [loc (m/down zloc)
         parent zloc]
    (if-not (and loc (z/node loc))
      parent
      (if-let [v0 (m/right loc)]
        (if-let [v (f v0)]
          (recur (m/right v) (m/up v))
          (recur (m/right v0) parent))
        parent))))

(defn map-keys
  "Apply function to all key nodes of the given map node."
  [f zloc]
  {:pre [(map? zloc)]}
  (loop [loc (m/down zloc)
         parent zloc]
    (if-not (and loc (z/node loc))
      parent
      (if-let [v (f loc)]
        (recur (m/right (m/right v)) (m/up v))
        (recur (m/right (m/right loc)) parent)))))

(defn map
  "Apply function to all value nodes in the given seq node. Iterates over
   value nodes of maps but over each element of a seq."
  [f zloc]
  {:pre [(seq? zloc)]}
  (if (map? zloc)
    (map-vals f zloc)
    (map-seq f zloc)))

;; ## Get/Assoc

(defn get
  "If a map is given, get element with the given key; if a seq is given, get nth element."
  [zloc k]
  {:pre [(or (map? zloc) (and (seq? zloc) (integer? k)))]}
  (if (map? zloc)
    (some-> zloc m/down (f/find-value k) m/right)
    (nth
      (some->> (m/down zloc)
               (iterate m/right)
               (take-while identity))
      k)))

(defn assoc
  "Set map/seq element to the given value."
  [zloc k v]
  (if-let [vloc (get zloc k)]
    (-> vloc (e/replace v) m/up)
    (if (map? zloc)
      (-> zloc
          (i/append-child k)
          (i/append-child v))
      (throw
        (IndexOutOfBoundsException.
          (format "index out of bounds: %d" k))))))
