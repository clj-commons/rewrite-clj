(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Nodes

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [_this]
    tag)
  (node-type [_n] :seq)
  (printable-only? [_] false)
  (sexpr* [_node opts]
    (seq-fn (node/sexprs children opts)))
  (length [_]
    (+ wrap-length (node/sum-lengths children)))
  (string [_this]
    (->> (node/concat-strings children)
         (interop/simple-format format-string)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    (dec wrap-length))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! SeqNode)

;; ## Constructors

(defn list-node
  "Create a node representing an EDN list."
  [children]
  (->SeqNode :list "(%s)" 2 #(apply list %) children))

(defn vector-node
  "Create a node representing an EDN vector."
  [children]
  (->SeqNode :vector "[%s]" 2 vec children))

(defn set-node
  "Create a node representing an EDN set."
  [children]
  (->SeqNode :set "#{%s}" 3 set children))

(defn map-node
  "Create a node representing an EDN map."
  [children]
  (->SeqNode :map "{%s}" 2 #(apply hash-map %) children))