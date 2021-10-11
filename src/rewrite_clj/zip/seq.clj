;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/zip/seq.clj
(ns ^:no-doc rewrite-clj.zip.seq
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.zip.seqz] ))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn seq?
  "Returns true if current node in `zloc` is a sequence."
  [zloc] (rewrite-clj.zip.seqz/seq? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn list?
  "Returns true if current node in `zloc` is a list."
  [zloc] (rewrite-clj.zip.seqz/list? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn vector?
  "Returns true if current node in `zloc` is a vector."
  [zloc] (rewrite-clj.zip.seqz/vector? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn set?
  "Returns true if current node in `zloc` is a set."
  [zloc] (rewrite-clj.zip.seqz/set? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map?
  "Returns true if current node in `zloc` is a map."
  [zloc] (rewrite-clj.zip.seqz/map? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map-vals
  "Returns `zloc` with function `f` applied to each value node of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at value node and should return:
  - an updated zloc with zloc positioned at value node
  - a falsey value to leave value node unchanged

  Folks typically use [[edit]] for `f`."
  [f zloc] (rewrite-clj.zip.seqz/map-vals f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map-keys
  "Returns `zloc` with function `f` applied to all key nodes of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at key node and should return:
  - an updated zloc with zloc positioned at key node
  - a falsey value to leave value node unchanged

  Folks typically use [[rewrite-clj.zip/edit]] for `f`."
  [f zloc] (rewrite-clj.zip.seqz/map-keys f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
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
  [f zloc] (rewrite-clj.zip.seqz/map f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn get
  "Returns `zloc` located to map key node's sexpr value matching `k` else `nil`.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k] (rewrite-clj.zip.seqz/get zloc k))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn assoc
  "Returns `zloc` with current node's `k` set to value `v`.

  `zloc` location is unchanged.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences, an exception is thrown if index is out of bounds

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k v] (rewrite-clj.zip.seqz/assoc zloc k v))
