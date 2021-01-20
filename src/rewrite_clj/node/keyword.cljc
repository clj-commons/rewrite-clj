(ns ^:no-doc rewrite-clj.node.keyword
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord KeywordNode [k namespaced?]
  node/Node
  (tag [_] :token)
  (printable-only? [_] false)
  (sexpr [_]
    (if (and namespaced?
             (not (namespace k)))
      (keyword
        (name #?(:clj (ns-name *ns*) :cljs (throw (ex-info "coming soon" {}))))
        (name k))
      k))
  (length [_this]
    (let [c (inc (count (name k)))]
      (if namespaced?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_]
    (str (when namespaced? ":")
         (pr-str k)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! KeywordNode)

;; ## Constructor

(defn keyword-node
  "Create node representing a keyword. If `namespaced?` is given as `true`
   a keyword à la `::x` or `::ns/x` (i.e. namespaced/aliased) is generated."
  [k & [namespaced?]]
  {:pre [(keyword? k)]}
  (->KeywordNode k namespaced?))
