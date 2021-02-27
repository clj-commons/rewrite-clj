;; DO NOT EDIT FILE, automatically generated from: template/rewrite_clj/zip/find.clj
(ns ^:no-doc rewrite-clj.zip.find
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [find])
  (:require [rewrite-clj.zip.findz]))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find
  "Return `zloc` located to the first node satisfying predicate `p?` else nil.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-last-by-pos
  "Return `zloc` located to the last node spanning position `pos` that satisfies predicate `p?` else `nil`.
   Search is depth-first from the current node.

  NOTE: Does not ignore whitespace/comment nodes."
  ([zloc pos] (rewrite-clj.zip.findz/find-last-by-pos zloc pos))
  ([zloc pos p?] (rewrite-clj.zip.findz/find-last-by-pos zloc pos p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-depth-first
  "Return `zloc` located to the first node satisfying predicate `p?` else `nil`.
   Search is depth-first from the current node."
  [zloc p?] (rewrite-clj.zip.findz/find-depth-first zloc p?))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next
  "Return `zloc` located to the next node satisfying predicate `p?` else `nil`.
   Search starts one movement `f` from the current node continues via `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-next zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-next zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-depth-first
  "Return `zloc` located to next node satisfying predicate `p?` else `nil`.
   Search starts depth-first after the current node."
  [zloc p?] (rewrite-clj.zip.findz/find-next-depth-first zloc p?))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-tag
  "Return `zloc` located to the first node with tag `t` else `nil`.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc t] (rewrite-clj.zip.findz/find-tag zloc t))
  ([zloc f t] (rewrite-clj.zip.findz/find-tag zloc f t)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-tag
  "Return `zloc` located to the next node with tag `t` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc t] (rewrite-clj.zip.findz/find-next-tag zloc t))
  ([zloc f t] (rewrite-clj.zip.findz/find-next-tag zloc f t)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-tag-by-pos
  "Return `zloc` located to the last node spanning position `pos` with tag `t` else `nil`.
  Search is depth-first from the current node."
  [zloc pos t] (rewrite-clj.zip.findz/find-tag-by-pos zloc pos t))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-token
  "Return `zloc` located to the the first token node satisfying predicate `p?`.
  Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-token zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-token zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-token
  "Return `zloc` location to the next token node satisfying predicate `p?` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[rewrite-clj.zip/right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-next-token zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-next-token zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-value
  "Return `zloc` located to the first token node that `sexpr`esses to `v` else `nil`.
   Search starts from the current node and continues via movement function `f`.

   `v` can be a single value or a set. When `v` is a set, matches on any value in set.

   `f` defaults to [[rewrite-clj.zip/right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v] (rewrite-clj.zip.findz/find-value zloc v))
  ([zloc f v] (rewrite-clj.zip.findz/find-value zloc f v)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-value
  "Return `zloc` located to the next token node that `sexpr`esses to `v` else `nil`.
   Search starts one movement `f` from the current location, and continues via `f`.

   `v` can be a single value or a set. When `v` is a set matches on any value in set.

   `f` defaults to [[rewrite-clj.zip/right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v] (rewrite-clj.zip.findz/find-next-value zloc v))
  ([zloc f v] (rewrite-clj.zip.findz/find-next-value zloc f v)))
