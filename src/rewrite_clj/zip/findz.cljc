(ns ^:no-doc rewrite-clj.zip.findz
  (:refer-clojure :exclude [find])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Helpers

(defn- tag-predicate
  [t & [additional]]
  (if additional
    (fn [node]
      (and (= (base/tag node) t)
           (additional node)))
    #(= (base/tag %) t)))

(defn- position-in-range? [zloc pos]
  (let [[r c] (if (map? pos) [(:row pos) (:col pos)] pos)]
    (when (or (<= r 0) (<= c 0))
      (throw (ex-info "zipper row and col positions are ones-based" {:pos pos})))
    (let [[[zstart-row zstart-col][zend-row zend-col]] (zraw/position-span zloc)]
      (and (>= r zstart-row)
           (<= r zend-row)
           (if (= r zstart-row) (>= c zstart-col) true)
           (if (= r zend-row) (< c zend-col) true)))))

;; ## Find Operations

(defn find
  "Return `zloc` located to the first node satisfying predicate `p?` else nil.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc p?]
   (find zloc m/right p?))
  ([zloc f p?]
   (->> zloc
        (iterate f)
        (take-while identity)
        (take-while (complement m/end?))
        (drop-while (complement p?))
        (first))))

(defn find-last-by-pos
  "Return `zloc` located to the last node spanning position `pos` that satisfies predicate `p?` else `nil`.
   Search is depth-first from the current node.

  NOTE: Does not ignore whitespace/comment nodes."
  ([zloc pos] (find-last-by-pos zloc pos (constantly true)))
  ([zloc pos p?]
   (->> zloc
        (iterate zraw/next)
        (take-while identity)
        (take-while (complement m/end?))
        (filter #(and (p? %)
                      (position-in-range? % pos)))
        last)))

(defn find-depth-first
  "Return `zloc` located to the first node satisfying predicate `p?` else `nil`.
   Search is depth-first from the current node."
  [zloc p?]
  (find zloc m/next p?))

(defn find-next
  "Return `zloc` located to the next node satisfying predicate `p?` else `nil`.
   Search starts one movement `f` from the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc p?]
   (find-next zloc m/right p?))
  ([zloc f p?]
   (some-> zloc f (find f p?))))

(defn find-next-depth-first
  "Return `zloc` located to next node satisfying predicate `p?` else `nil`.
   Search starts depth-first after the current node."
  [zloc p?]
  (find-next zloc m/next p?))

(defn find-tag
  "Return `zloc` located to the first node with tag `t` else `nil`.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc t]
   (find-tag zloc m/right t))
  ([zloc f t]
   (find zloc f #(= (base/tag %) t))))

(defn find-next-tag
  "Return `zloc` located to the next node with tag `t` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc t]
   (find-next-tag zloc m/right t))
  ([zloc f t]
   (->> (tag-predicate t)
        (find-next zloc f))))

(defn find-tag-by-pos
  "Return `zloc` located to the last node spanning position `pos` with tag `t` else `nil`.
  Search is depth-first from the current node."
  ([zloc pos t]
   (find-last-by-pos zloc pos #(= (base/tag %) t))))

(defn find-token
  "Return `zloc` located to the the first token node satisfying predicate `p?`.
  Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc p?]
   (find-token zloc m/right p?))
  ([zloc f p?]
   (->> (tag-predicate :token p?)
        (find zloc f))))

(defn find-next-token
  "Return `zloc` located to the next token node satisfying predicate `p?` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc p?]
   (find-next-token zloc m/right p?))
  ([zloc f p?]
   (find-token (f zloc) f p?)))

(defn find-value
  "Return `zloc` located to the first token node that `sexpr`esses to `v` else `nil`.
   Search starts from the current node and continues via movement function `f`.

   `v` can be a single value or a set. When `v` is a set, matches on any value in set.

   `f` defaults to [[right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v]
   (find-value zloc m/right v))
  ([zloc f v]
   (let [p? (if (set? v)
              (comp v base/sexpr)
              #(= (base/sexpr %) v))]
     (find-token zloc f p?))))

(defn find-next-value
  "Return `zloc` located to the next token node that `sexpr`esses to `v` else `nil`.
   Search starts one movement `f` from the current location, and continues via `f`.

   `v` can be a single value or a set. When `v` is a set matches on any value in set.

   `f` defaults to [[right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v]
   (find-next-value zloc m/right v))
  ([zloc f v]
   (find-value (f zloc) f v)))
