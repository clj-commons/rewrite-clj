(ns rewrite-clj.node.reader-macro
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord ReaderNode [tag prefix sexpr-fn
                       sexpr-count children]
  node/Node
  (tag [_] tag)
  (printable-only? [_]
    (not sexpr-fn))
  (sexpr [_]
    (if sexpr-fn
      (sexpr-fn (node/sexprs children))
      (throw (UnsupportedOperationException.))))
  (string [_]
    (str "#" prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (node/assert-sexpr-count children' sexpr-count)
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(defrecord ReaderMacroNode [children]
  node/Node
  (tag [_] :reader-macro)
  (printable-only?[_] false)
  (sexpr [this]
    (list 'read-string (node/string this)))
  (string [_]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(defrecord DerefNode [children]
  node/Node
  (tag [_] :deref)
  (printable-only?[_] false)
  (sexpr [this]
    (list* 'deref (node/sexprs children)))
  (string [_]
    (str "@" (node/concat-strings children)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 1)
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! ReaderNode)
(node/make-printable! ReaderMacroNode)
(node/make-printable! DerefNode)

;; ## Constructors

(defn- ->node
  [tag prefix sexpr-fn sexpr-count children]
  (node/assert-sexpr-count children sexpr-count)
  (->ReaderNode
    tag prefix sexpr-fn sexpr-count children))

(defn var-node
  [children]
  (->node :var "'" #(list* 'var %) 1 children))

(defn fn-node
  [child]
  (->node :fn "" nil 1 [child]))

(defn eval-node
  [children]
  (->node
    :eval "="
    #(list 'eval (list 'quote %))
    1 children))

(defn uneval-node
  [children]
  (->node :uneval "_" nil 1 children))

(defn reader-macro-node
  [children]
  (->ReaderMacroNode children))

(defn deref-node
  [children]
  (->DerefNode children))
