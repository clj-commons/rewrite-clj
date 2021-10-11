;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/zip/edit.clj
(ns ^:no-doc rewrite-clj.zip.edit
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [replace])
  (:require [rewrite-clj.zip.editz]))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn replace
  "Return `zloc` with the current node replaced by `value`.
  If `value` is not already a node, an attempt will be made to coerce it to one.

  Use [[replace*]] for non-coercing version of replace."
  [zloc value] (rewrite-clj.zip.editz/replace zloc value))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn edit
  "Return `zloc` with the current node replaced with the result of:

   `(apply f (s-expr current-node) args)`

  The result of `f`, if not already a node, will be coerced to a node if possible.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances).

  Use [[edit*]] for non-coercing version of edit."
  [zloc f & args] (apply rewrite-clj.zip.editz/edit zloc f args))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn splice
  "Return zipper with the children of the current node in `zloc` merged into itself.
   (akin to Clojure's `unquote-splicing` macro: `~@...`).
   - if the node is not one that can have children, no modification will
     be performed.
   - if the node has no or only whitespace children, it will be removed.
   - otherwise, splicing will be performed, moving the zipper to the first
     non-whitespace spliced child node.

  For example, given `[[1 2 3] 4 5 6]`, if zloc is located at vector `[1 2 3]`, a splice will result in raising the vector's children up `[1 2 3 4 5 6]` and locating the zipper at node `1`."
  [zloc] (rewrite-clj.zip.editz/splice zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn prefix
  "Return zipper with the current node in `zloc` prefixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, first line is prefixed."
  [zloc s] (rewrite-clj.zip.editz/prefix zloc s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn suffix
  "Return zipper with the current node in `zloc` suffixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, last line is suffixed."
  [zloc s] (rewrite-clj.zip.editz/suffix zloc s))
