(ns rewrite-clj.node.set
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.record-base :refer [defrecord-from-base]]))

(defrecord-from-base SetNode node/CollBase [children]
  node/Node
  (tag [_]
    :set)
  (sexpr [this]
    (set (node/sexprs children)))
  
  node/EnclosedForm
  (format-string [this]
    "#{%s}"))

(node/make-printable! SetNode)

;; ## Constructor

(defn set-node
  "Create a node representing an EDN set."
  [children]
  (->SetNode children))
