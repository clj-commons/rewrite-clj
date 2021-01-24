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
  (tag [_node] tag)
  (node-type [_node] :seq)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (seq-fn (node/sexprs children opts)))
  (length [_node]
    (+ wrap-length (node/sum-lengths children)))
  (string [_node]
    (->> (node/concat-strings children)
         (interop/simple-format format-string)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (assoc node :children children'))
  (leader-length [_node]
    (dec wrap-length))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! SeqNode)

;; ## Constructors

(defn list-node
  "Create a node representing a list with `children`."
  [children]
  (->SeqNode :list "(%s)" 2 #(apply list %) children))

(defn vector-node
  "Create a node representing a vector with `children`."
  [children]
  (->SeqNode :vector "[%s]" 2 vec children))

(defn set-node
  "Create a node representing a set with `children`."
  [children]
  (->SeqNode :set "#{%s}" 3 set children))

(defn map-node
  "Create a node representing an map with `children`."
  [children]
  (->SeqNode :map "{%s}" 2 #(apply hash-map %) children))