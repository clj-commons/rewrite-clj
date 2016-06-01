(ns rewrite-clj.node.vector
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.record-base :refer [defrecord-from-base]]))

(defrecord-from-base VectorNode node/CollBase [children]
  node/Node
  (tag [_]
    :vector)
  (sexpr [_]
    (vec (node/sexprs children)))
  
  node/EnclosedForm
  (format-string [this]
    "[%s]"))

(node/make-printable! VectorNode)

;; ## Constructor

(defn vector-node
  "Create a node representing an EDN vector."
  [children]
  (->VectorNode children))
