(ns rewrite-clj.node.meta
  (:require [rewrite-clj.node
             [protocols :as node]
             [whitespace :as ws]]))

;; ## Node

(defrecord MetaNode [tag prefix children]
  node/Node
  (tag [_] tag)
  (printable-only? [_] false)
  (sexpr [_]
    (let [[mta data] (node/sexprs children)]
      (with-meta data mta)))
  (string [_]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! MetaNode)

;; ## Constructor

(defn meta-node
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta "^" children))
  ([metadata data]
   (meta-node [metadata (ws/spaces 1) data])))

(defn raw-meta-node
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta* "#^" children))
  ([metadata data]
   (raw-meta-node [metadata (ws/spaces 1) data])))
