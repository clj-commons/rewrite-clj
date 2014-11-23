(ns rewrite-clj.node.quote
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord QuoteNode [tag prefix sym children]
  node/Node
  (tag [_] tag)
  (printable-only? [_] false)
  (sexpr [_]
    (list sym (first (node/sexprs children))))
  (string [_]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (node/assert-single-sexpr children')
    (assoc this :children (first children')))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! QuoteNode)

;; ## Constructors

(defn quote-node
  [children]
  (node/assert-single-sexpr children)
  (->QuoteNode
    :quote "'" `quote
    children))

(defn syntax-quote-node
  [children]
  (node/assert-single-sexpr children)
  (->QuoteNode
    :syntax-quote "`" `quote
    children))

(defn unquote-node
  [children]
  (node/assert-single-sexpr children)
  (->QuoteNode
    :unquote "~" `unquote
    children))

(defn unquote-splicing-node
  [children]
  (node/assert-single-sexpr children)
  (->QuoteNode
    :unquote-splicing "~@" `unquote-splicing
    children))
