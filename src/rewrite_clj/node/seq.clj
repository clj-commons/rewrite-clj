(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(declare seq-fn)

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this]
    ((seq-fn this) (node/sexprs children)))
  (length [_]
    (+ wrap-length (node/sum-lengths children)))
  (string [this]
    (->> (node/concat-strings children)
         (format format-string)))

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

(defn- seq-fn [^SeqNode n]
  (case (.tag n)
    :list     #(apply list %)
    :vector   vec
    :set      set
    :map      #(apply hash-map %)))


;; ## Constructors

(defn list-node
  "Create a node representing an EDN list."
  [children]
  (->SeqNode :list "(%s)" 2 children))

(defn vector-node
  "Create a node representing an EDN vector."
  [children]
  (->SeqNode :vector "[%s]" 2 children))

(defn set-node
  "Create a node representing an EDN set."
  [children]
  (->SeqNode :set "#{%s}" 3 children))

(defn map-node
  "Create a node representing an EDN map."
  [children]
  (->SeqNode :map "{%s}" 2 children))
