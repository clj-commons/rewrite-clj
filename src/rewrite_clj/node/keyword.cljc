(ns ^:no-doc rewrite-clj.node.keyword
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord KeywordNode [k auto-resolved?]
  node/Node
  (tag [_] :token)
  (node-type [_n] :keyword)
  (printable-only? [_] false)
  (sexpr* [_ opts]
    (if (and auto-resolved?
             (not (namespace k)))
      (keyword
        (name #?(:clj (ns-name *ns*) :cljs (throw (ex-info "coming soon" {}))))
        (name k))
      k))
  (length [_this]
    (let [c (inc (count (name k)))]
      (if auto-resolved?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_]
    (str (when auto-resolved? ":")
         (pr-str k)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! KeywordNode)

(defn keyword-node? [n]
  (= :keyword (node/node-type n)))

;; ## Constructor

(defn keyword-node
  "Create node representing a keyword. If `auto-resolved?` is given as `true`
   a keyword à la `::x` or `::ns/x` (i.e. namespaced/aliased) is generated."
  [k & [auto-resolved?]]
  {:pre [(keyword? k)]}
  (->KeywordNode k auto-resolved?))
