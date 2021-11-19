(ns ^:no-doc rewrite-clj.zip.editz
  (:refer-clojure :exclude [replace])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.token :as ntoken]
            [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.options :as options]
            [rewrite-clj.zip.removez :as r]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## In-Place Modification

(defn replace
  "Return `zloc` with the current node replaced by `item`.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[replace*]] for non-coercing version of replace."
  [zloc item]
  (zraw/replace zloc (node/coerce item)))

(defn- node-editor
  "Create s-expression from node, apply the function and create
   node from the result."
  [opts]
  (fn [node f]
    (-> (node/sexpr node opts)
        (f)
        (node/coerce))))

(defn edit
  "Return `zloc` with the current node replaced with the result of:

   `(apply f (s-expr current-node) args)`

  The result of `f`, if not already a node, will be coerced to a node if possible.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances).

  Use [[edit*]] for non-coercing version of edit."
  [zloc f & args]
  (zraw/edit zloc (node-editor (options/get-opts zloc)) #(apply f % args)))

;; ## Splice

(defn splice
  "Return zipper with the children of the current node in `zloc` merged into itself.
   (akin to Clojure's `unquote-splicing` macro: `~@...`).
   - if the node is not one that can have children, no modification will
     be performed.
   - if the node has no or only whitespace children, it will be removed.
   - otherwise, splicing will be performed, moving the zipper to the first
     non-whitespace spliced child node.

  For example, given `[[1 2 3] 4 5 6]`, if zloc is located at vector `[1 2 3]`, a splice will result in raising the vector's children up `[1 2 3 4 5 6]` and locating the zipper at node `1`."
  [zloc]
  (if (zraw/branch? zloc)
    (if-let [children (->> (zraw/children zloc)
                           (drop-while nwhitespace/whitespace?)
                           (reverse)
                           (drop-while nwhitespace/whitespace?)
                           (seq))]
      (let [loc (->> (reduce zraw/insert-right zloc children)
                     (u/remove-and-move-right))]
        (or (ws/skip-whitespace loc) loc))
      (r/remove zloc))
    zloc))

;; ## Prefix/Suffix

(defn- edit-token
  [zloc str-fn]
  (let [e (base/sexpr zloc)
        e' (cond (string? e) (str-fn e)
                 (keyword? e) (keyword (namespace e) (str-fn (name e)))
                 (symbol? e) (symbol (namespace e) (str-fn (name e))))]
    (zraw/replace zloc (ntoken/token-node e'))))

(defn- edit-multi-line
  [zloc line-fn]
  (let [n (-> (zraw/node zloc)
              (update-in [:lines] (comp line-fn vec)))]
    (zraw/replace zloc n)))

(defn prefix
  "Return zipper with the current node in `zloc` prefixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, first line is prefixed."
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str s %))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (update-in lines [0] #(str s %))))
                     (edit-multi-line zloc))))

(defn suffix
  "Return zipper with the current node in `zloc` suffixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, last line is suffixed."
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str % s))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (concat (butlast lines) [(str (last lines) s)])))
                     (edit-multi-line zloc))))
