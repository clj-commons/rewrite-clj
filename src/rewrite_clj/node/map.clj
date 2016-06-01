(ns rewrite-clj.node.map
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.record-base :refer [defrecord-from-base]]))

(defrecord-from-base MapNode node/CollBase [children]
  node/Node
  (tag [_]
    :map)
  (sexpr [this]
    (apply hash-map (node/sexprs children)))
  
  node/EnclosedForm
  (format-string [this]
    "{%s}"))

(node/make-printable! MapNode)

;; ## Constructor

(defn map-node
  "Create a node representing an EDN map."
  [children]
  (->MapNode children))
