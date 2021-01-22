(ns ^:no-doc rewrite-clj.node.meta
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord MetaNode [tag prefix children]
  node/Node
  (tag [_] tag)
  (node-type [_node] :meta)
  (printable-only? [_] false)
  (sexpr* [_ opts]
    (let [[mta data] (node/sexprs children opts)]
      (assert (interop/meta-available? data)
              (str "cannot attach metadata to: " (pr-str data)))
      (vary-meta data merge (if (map? mta) mta {mta true}))))
  (length [_]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))
  (leader-length [_]
    (count prefix))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! MetaNode)

;; ## Constructor

(defn meta-node
  "Create node representing a form and its metadata."
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta "^" children))
  ([metadata data]
   (meta-node [metadata (ws/spaces 1) data])))

(defn raw-meta-node
  "Create node representing a form and its metadata using the
   `#^` prefix."
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta* "#^" children))
  ([metadata data]
   (raw-meta-node [metadata (ws/spaces 1) data])))
