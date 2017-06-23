(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Nodes

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this]
    (seq-fn (node/sexprs children)))
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

(defrecord NamespacedMapNode [wrap-length children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    (let [n (node/sexpr (first children))]
      (reduce-kv
       (fn [m k v]
         (if (keyword? k)
           (cond (namespace k)
                 (assoc m k v)
                 (namespace n)
                 (assoc m (keyword
                           (str (namespace n) "/" (name n))
                           (name k)) v)
                 :else
                 (assoc m (keyword (name n) (name k)) v))
           (assoc m k v)))
       {} (apply hash-map (node/sexprs (-> children second :children))))))
  (length [_]
    (+ wrap-length (node/sum-lengths children)))
  (string [this]
    (format "#%s{%s}"
            (node/string (first children))
            (node/concat-strings (-> children second :children))))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))
  (leader-length [_]
    (dec wrap-length))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! SeqNode)
(node/make-printable! NamespacedMapNode)

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

(defn namespaced-map-node
  "Create a node representing an EDN map namespace."
  [children]
  (node/assert-sexpr-count children 2)
  (->NamespacedMapNode 2 children))
