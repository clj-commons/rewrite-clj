(ns ^:no-doc rewrite-clj.node.quote
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord QuoteNode [tag prefix sym children]
  node/Node
  (tag [_node] tag)
  (node-type [_node] :quote)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (list sym (first (node/sexprs children opts))))
  (length [_node]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_node]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (node/assert-single-sexpr children')
    (assoc node :children children'))
  (leader-length [_node]
    (count prefix))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! QuoteNode)

;; ## Constructors

(defn- ->node
  [t prefix sym children]
  (node/assert-single-sexpr children)
  (->QuoteNode t prefix sym children))

(defn quote-node
  "Create node representing a quoted form where `children`
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :quote "'" 'quote
      children)
    (recur [children])))

(defn syntax-quote-node
  "Create node representing a syntax-quoted form where `children`
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :syntax-quote "`" 'quote
      children)
    (recur [children])))

(defn unquote-node
  "Create node representing an unquoted form (i.e. `~...`) where `children`.
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :unquote "~" 'unquote
      children)
    (recur [children])))

(defn unquote-splicing-node
  "Create node representing an unquote-spliced form (i.e. `~@...`) where `children`.
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :unquote-splicing "~@" 'unquote-splicing
      children)
    (recur [children])))
