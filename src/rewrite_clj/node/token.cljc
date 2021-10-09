(ns ^:no-doc rewrite-clj.node.token
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- choose-qualifier [map-qualifier sym-qualifier]
  (when (not (and map-qualifier (= "_" (:prefix sym-qualifier))))
    (or sym-qualifier map-qualifier)))

(defn- symbol-qualifier [value]
  (when (and (symbol? value) (namespace value))
    {:prefix (namespace value)}))

;; A symbol is different than a keyword in that it can only be auto-resolve qualified by a namespaced map
(defn- symbol-sexpr [value map-qualifier {:keys [auto-resolve]}]
  (let [q (choose-qualifier map-qualifier (symbol-qualifier value))]
    (symbol (some-> (if (:auto-resolved? q)
                      ((or auto-resolve node/default-auto-resolve)
                       (or (some-> (:prefix q) symbol)
                           :current))
                      (:prefix q))
                    str)
            (name value))))

(defrecord TokenNode [value string-value]
  node/Node
  (tag [_ndoe] :token)
  (node-type [_node] :token)
  (printable-only? [_node] false)
  (sexpr* [_node _opts] value)
  (length [_node]
    (count string-value))
  (string [_node] string-value)

  Object
  (toString [node]
    (node/string node)))

(defrecord SymbolNode [value string-value map-qualifier]
  node/Node
  (tag [_node] :token)
  (node-type [_node] :symbol)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (symbol-sexpr value map-qualifier opts))
  (length [_node] (count string-value))
  (string [_node] string-value)

  node/MapQualifiable
  (map-context-apply [node map-qualifier]
    (assoc node :map-qualifier map-qualifier))
  (map-context-clear [node]
    (assoc node :map-qualifier nil))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! TokenNode)
(node/make-printable! SymbolNode)

(defn symbol-node?
  "Returns true if `n` is a node representing a symbol."
  [n]
  (= :symbol (node/node-type n)))

;; ## Constructor

(defn token-node
  "Create node for an unspecified token of `value`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/token-node 'sym) n/string)
   ;; => \"sym\"

   (-> (n/token-node 42) n/string)
   ;; => \"42\"
   ```"
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
    (if (symbol? value)
      (->SymbolNode value string-value nil)
      (->TokenNode value string-value))))
