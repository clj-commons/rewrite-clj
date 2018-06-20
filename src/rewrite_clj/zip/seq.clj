(ns ^:no-doc rewrite-clj.zip.seq
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.zip
             [base :as base]
             [edit :as e]
             [find :as f]
             [insert :as i]
             [move :as m]]
            [rewrite-clj.node :as n]
            [rewrite-clj.custom-zipper.core :as z]))

;; ## Predicates

(defn seq?
  [zloc]
  (contains?
    #{:forms :list :vector :set :map}
    (base/tag zloc)))

(defn list?
  [zloc]
  (= (base/tag zloc) :list))

(defn vector?
  [zloc]
  (= (base/tag zloc) :vector))

(defn set?
  [zloc]
  (= (base/tag zloc) :set))

(defn map?
  [zloc]
  (= (base/tag zloc) :map))

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

(defn assoc-in
  "Set (nested) map element to the given value."
  [zloc [k & ks] v]
  (if ks
    (if-let [vloc (get zloc k)]
      (let [val (assoc-in (->> vloc
                               (remove n/printable-only?)
                               first
                               n/string
                               base/of-string) ks v)]
        (assoc zloc k (z/root val)))
      (assoc zloc k (clojure.core/assoc-in {} ks v)))
    (assoc zloc k v)))
