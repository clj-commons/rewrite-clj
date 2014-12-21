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
  (length [this]
    (let [c (inc (count (name k)))]
      (if namespaced?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_]
    (str (if namespaced? ":")
         (pr-str k)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! KeywordNode)

;; ## Constructor

(defn keyword-node
  [k & [namespaced?]]
  {:pre [(keyword? k)]}
  (assert (or (not namespaced?) (not (namespace k)))
          (str "invalid namespaced keyword: :" (pr-str k)))
  (->KeywordNode k namespaced?))
