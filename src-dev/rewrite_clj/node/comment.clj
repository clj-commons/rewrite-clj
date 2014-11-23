(ns rewrite-clj.node.comment
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord CommentNode [s]
  node/Node
  (tag [_] :comment)
  (printable-only? [_] true)
  (sexpr [_]
    (throw (UnsupportedOperationException.)))
  (string [_]
    (str ";" s))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! CommentNode)

;; ## Constructor

(defn comment-node
  "Create node representing an EDN comment."
  [s]
  {:pre [(re-matches #"[^\r\n]+[\r\n]?" s)]}
  (->CommentNode s))
