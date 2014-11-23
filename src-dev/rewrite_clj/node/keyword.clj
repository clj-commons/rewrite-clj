(ns rewrite-clj.node.keyword
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord KeywordNode [k namespaced?]
  node/Node
  (tag [_] :token)
  (printable-only? [_] false)
  (sexpr [_]
    (if (and namespaced?
             (not (namespace k)))
      (keyword
        (name (ns-name *ns*))
        (name k))
      k))
  (string [_]
    (str (if namespaced?  ":")
         (pr-str k)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! KeywordNode)

;; ## Constructor

(defn keyword-node
  [k & [namespaced?]]
  {:pre [(keyword? k)]}
  (->KeywordNode k namespaced?))
