(ns rewrite-clj.node.list
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.record-base :refer [defrecord-from-base]]))

(defrecord-from-base ListNode node/CollBase [children]
  node/Node
  (tag [_]
    :list)
  (sexpr [this]
    (apply list (node/sexprs children)))
  
  node/EnclosedForm
  (format-string [this]
    "(%s)"))

(node/make-printable! ListNode)


;; ## Constructor

(defn list-node
  "Create a node representing an EDN list."
  [children]
  (->ListNode children))
