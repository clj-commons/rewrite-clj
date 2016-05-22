(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(declare
  seq-fn 
  seq-format-string
  seq-wrap-length)

(defrecord SeqNode [tag
                    children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this]
    ((seq-fn this) (node/sexprs children)))
  (length [this]
    (+ (seq-wrap-length this)
       (node/sum-lengths children)))
  (string [this]
    (->> (node/concat-strings children)
         (format (seq-format-string this))))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [this]
    (dec (seq-wrap-length this)))

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

(defn- seq-format-string [^SeqNode n]
  (case (.tag n)
    :list     "(%s)"
    :vector   "[%s]"
    :set      "#{%s}"
    :map      "{%s}"))

(defn- seq-wrap-length [^SeqNode n]
  (case (.tag n)
    (case (.tag n)
    :list     2
    :vector   2
    :set      3
    :map      2)))


;; ## Constructors

(defn list-node
  "Create a node representing an EDN list."
  [children]
  (->SeqNode :list children))

(defn vector-node
  "Create a node representing an EDN vector."
  [children]
  (->SeqNode :vector children))

(defn set-node
  "Create a node representing an EDN set."
  [children]
  (->SeqNode :set children))

(defn map-node
  "Create a node representing an EDN map."
  [children]
  (->SeqNode :map children))
