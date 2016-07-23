(ns rewrite-clj.node.colls
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.record-base :refer [defrecord-from-base]]))


(def nodes
  [{:symbol        'VectorNode
    :tag           :list
    :format-string "[]"
    :sexpr-expr    '(vec (node/sexprs children))}
   
   {:symbol        'ListNode
    :tag           :list
    :format-string "()"
    :sexpr-expr    '(apply list (node/sexprs children))}
   
   {:symbol        'SetNode
    :tag           :set
    :format-string "#{}"
    :sexpr-expr    '(set (node/sexprs children))}
   
   {:symbol        'MapNode
    :tag           :map
    :format-string "{}"
    :sexpr-expr    '(apply hash-map (node/sexprs children))}])

(defmacro ^:private def-all-nodes []
  `(do ~@(for [n nodes]
           `(do
              (defrecord-from-base ~(:symbol n) node/CollBase [children]
                node/Node
                (tag [_]
                     (:tag n))
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
                (->VectorNode children))))))

(def-all-nodes)
