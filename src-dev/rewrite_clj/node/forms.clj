(ns rewrite-clj.node.forms
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord FormsNode [children]
  node/Node
  (tag [_]
    :forms)
  (printable-only? [_]
    false)
  (sexpr [_]
    (list* 'do (node/sexprs children)))
  (string [_]
    (node/concat-strings children))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! FormsNode)

;; ## Constructor

(defn forms-node
  [children]
  (->FormsNode children))
