(ns ^:no-doc rewrite-clj.node.comment
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord CommentNode [prefix s]
  node/Node
  (tag [_node] :comment)
  (node-type [_node] :comment)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [_node]
    (+ (count prefix) (count s)))
  (string [_node]
    (str prefix s))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! CommentNode)

;; ## Constructor

(defn comment-node
  "Create node representing a comment with text `s`.

   You may optionally specify a `prefix` of `\";\"` or `\"#!\"`, defaults is `\";\"`.

   Argument `s`:
   - must not include the `prefix`
   - usually includes the trailing newline character, otherwise subsequent nodes will be on the comment line

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/comment-node \"; my comment\\n\")
       n/string)
   ;; => \";; my comment\\n\"

   (-> (n/comment-node \"#!\" \"/usr/bin/env bb\\n\")
       n/string)
   ;; => \"#!/usr/bin/env bb\\n\"
   ```"
  ([s]
   (comment-node ";" s))
  ([prefix s]
   {:pre [(and (re-matches #"[^\r\n]*[\r\n]?" s)
               (or (= prefix ";") (= prefix "#!")))]}
   (->CommentNode prefix s)))

(defn comment?
  "Returns true if `node` is a comment."
  [node]
  (= (node/tag node) :comment))
