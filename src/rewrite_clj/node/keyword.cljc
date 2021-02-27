(ns ^:no-doc rewrite-clj.node.keyword
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- choose-qualifier [map-qualifier kw-qualifier]
  (when (not (and map-qualifier (= "_" (:prefix kw-qualifier))))
    (or kw-qualifier map-qualifier)))

(defn kw-qualifier [k auto-resolved?]
  (when (or auto-resolved? (namespace k))
    {:auto-resolved? auto-resolved?
     :prefix (namespace k)}))

(defn keyword-sexpr [kw kw-auto-resolved? map-qualifier {:keys [auto-resolve]}]
  (let [q (choose-qualifier map-qualifier (kw-qualifier kw kw-auto-resolved?))]
    (keyword (some-> (if (:auto-resolved? q)
                       ((or auto-resolve node/default-auto-resolve)
                        (or (some-> (:prefix q) symbol)
                            :current))
                       (:prefix q))
                     str)
             (name kw))))

(defrecord KeywordNode [k auto-resolved? map-qualifier]
  node/Node
  (tag [_node] :token)
  (node-type [_node] :keyword)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (keyword-sexpr k auto-resolved? map-qualifier opts))
  (length [_node]
    (let [c (inc (count (name k)))]
      (if auto-resolved?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_node]
    (str (when auto-resolved? ":")
         (pr-str k)))

  node/MapQualifiable
  (map-context-apply [node map-qualifier]
    (assoc node :map-qualifier map-qualifier))
  (map-context-clear [node]
    (assoc node :map-qualifier nil))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! KeywordNode)

(defn keyword-node? 
  "Returns true if `n` is a node representing a keyword."
  [n]
  (= :keyword (node/node-type n)))

;; ## Constructor

(defn keyword-node
  "Create a node representing a keyword `k`.

  Examples usages:
  * `(keyword-node :kw)` - unqualified
  * `(keyword-node :my-prefix/kw)` - qualified

  You can pass `true` for the optional `auto-resolved?` parameter:
  * `(keyword-node :kw true)` - auto-resolved to current ns, equivalent to code `::kw`
  * `(keyword-node :ns-alias/kw true)` - auto-resolved to namespace with alias ns-alias, equivalent to code `::ns-alias/kw`"
  ([k auto-resolved?]
   {:pre [(keyword? k)]}
   (->KeywordNode k auto-resolved? nil))
  ([k] (keyword-node k false)))
