(ns ^:no-doc rewrite-clj.zip.seqz
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.editz :as e]
            [rewrite-clj.zip.findz :as f]
            [rewrite-clj.zip.insert :as i]
            [rewrite-clj.zip.move :as m]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Predicates

(defn seq?
  "Returns true if current node in `zloc` is a sequence."
  [zloc]
  (contains?
   #{:forms :list :vector :set :map :namespaced-map}
   (base/tag zloc)))

(defn list?
  "Returns true if current node in `zloc` is a list."
  [zloc]
  (= (base/tag zloc) :list))

(defn vector?
  "Returns true if current node in `zloc` is a vector."
  [zloc]
  (= (base/tag zloc) :vector))

(defn set?
  "Returns true if current node in `zloc` is a set."
  [zloc]
  (= (base/tag zloc) :set))

(defn map?
  "Returns true if current node in `zloc` is a map."
  [zloc]
  (= (base/tag zloc) :map))

(defn namespaced-map?
  "Returns true if the current node in `zloc` is a namespaced map."
  [zloc]
  (= (base/tag zloc) :namespaced-map))

;; ## Map Operations

(defn- map-seq
  [f zloc]
  {:pre [(seq? zloc)]}
  (if-let [zloc-n0 (m/down zloc)]
    (some->> (f zloc-n0)
             (iterate
              (fn [loc]
                (when-let [zloc-n (m/right loc)]
                  (f zloc-n))))
             (take-while identity)
             (last)
             (m/up))
    zloc))

(defn- map-vals* [f map-loc]
  (loop [loc (m/down map-loc)
         parent map-loc]
    (if-not (and loc (zraw/node loc))
      parent
      (if-let [zloc-map-value (m/right loc)]
        (if-let [new-zloc-map-value (f zloc-map-value)]
          (recur (m/right new-zloc-map-value) (m/up new-zloc-map-value))
          (recur (m/right zloc-map-value) parent))
        parent))))

(defn- map-loc [zloc]
  (if (namespaced-map? zloc)
    (-> zloc m/down m/rightmost)
    zloc))

(defn- container-loc [zloc map-loc]
  (if (namespaced-map? zloc)
    (-> map-loc m/up)
    map-loc))

(defn map-vals
  "Returns `zloc` with function `f` applied to each value node of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at value node and should return:
  - an updated zloc with zloc positioned at value node
  - a falsey value to leave value node unchanged

  Folks typically use [[edit]] for `f`."
  [f zloc]
  {:pre [(or (map? zloc) (namespaced-map? zloc))]}
  (container-loc zloc
                 (map-vals* f (map-loc zloc))))

(defn- map-keys* [f map-loc]
  (loop [loc (m/down map-loc)
         parent map-loc]
    (if-not (and loc (zraw/node loc))
      parent
      (if-let [zloc-map-key (f loc)]
        (recur (m/right (m/right zloc-map-key)) (m/up zloc-map-key))
        (recur (m/right (m/right loc)) parent)))))

(defn map-keys
  "Returns `zloc` with function `f` applied to all key nodes of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at key node and should return:
  - an updated zloc with zloc positioned at key node
  - a falsey value to leave value node unchanged

  Folks typically use [[rewrite-clj.zip/edit]] for `f`."
  [f zloc]
  {:pre [(or (map? zloc) (namespaced-map? zloc))]}
  (container-loc zloc
                 (map-keys* f (map-loc zloc))))

(defn map
  "Returns `zloc` with function `f` applied to all nodes of the current node.
  Current node must be a sequence node. Equivalent to [[rewrite-clj.zip/map-vals]] for maps.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at
  - value nodes for maps
  - each element of a seq
  and is should return:
  - an updated zloc with zloc positioned at edited node
  - a falsey value to leave value node unchanged

  Folks typically use [[edit]] for `f`."
  [f zloc]
  {:pre [(seq? zloc)]}
  (if (or (map? zloc) (namespaced-map? zloc))
    (map-vals f zloc)
    (map-seq f zloc)))

;; ## Get/Assoc

(defn get
  "Returns `zloc` located to map key node's sexpr value matching `k` else `nil`.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k]
  {:pre [(or (map? zloc) (namespaced-map? zloc) (and (seq? zloc) (integer? k)))]}
  (cond
    (map? zloc)
    (some-> zloc m/down (f/find-value k) m/right)

    (namespaced-map? zloc)
    (some-> zloc m/down m/rightmost m/down (f/find-value k) m/right)

    :else
    (nth
     (some->> (m/down zloc)
              (iterate m/right)
              (take-while identity))
     k)))

(defn assoc
  "Returns `zloc` with current node's `k` set to value `v`.

  `zloc` location is unchanged.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences, an exception is thrown if index is out of bounds

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k v]
  (container-loc zloc
                 (if-let [value-loc (get zloc k)]
                   (-> value-loc (e/replace v) m/up)
                   (if (or (map? zloc) (namespaced-map? zloc))
                     (-> (map-loc zloc)
                         (i/append-child k)
                         (i/append-child v))
                     (throw
                      (ex-info (str "index out of bounds: " k) {}))))))
