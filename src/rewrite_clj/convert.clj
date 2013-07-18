(ns ^{ :doc "Convert EDN tree to Clojure data."
       :author "Yannick Scherer"}
  rewrite-clj.convert
  (:require [clojure.walk :as w]))

(defn- filter-unnecessary
  "Filter unnecessary data from seq."
  [sq]
  (filter (comp (complement #{:comment :whitespace}) first) sq))

(defn- group-metadata
  "Group metadata with next token. Expects a seq of non-whitespace/
   non-comment elements."
  [sq]
  (loop [sq sq
         r []]
    (if-not (seq sq)
      (seq r)
      (let [[[k v :as x] & rst] sq]
        (if (= k :meta)
          (let [[tv & rst] rst]
            (recur rst (conj r [:meta v tv])))
        (recur rst (conj r x)))))))

(defn- normalize
  "Remove all unnecessary data from EDN tree, group metadata."
  [tree]
  (->> tree
    (w/postwalk
      (fn [x]
        (if (vector? x)
          (if (seq? (second x))
            [(first x) (-> (second x)
                         (filter-unnecessary)
                         (group-metadata))]
            x)
          x)))))

(defn value-of
  "Create value from EDN tree."
  [tree]
  (->> tree
    (normalize)
    (w/postwalk
      (fn [x]
        (if (vector? x)
          (let [[t v & rst] x]
              (condp = t
                :vector (vec v)
                :list (apply list v)
                :set (set v)
                :meta (with-meta (first rst) (if (keyword? v) { v true } v))
                :map (into {} (map vec (partition 2 v)))
                v))
          x)))))
