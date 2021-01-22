(ns ^:no-doc rewrite-clj.node.token
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord TokenNode [value string-value]
  node/Node
  (tag [_] :token)
  (node-type [_n] :token)
  (printable-only? [_] false)
  (sexpr* [_node _opts] value)
  (length [_] (count string-value))
  (string [_] string-value)

  Object
  (toString [this]
    (node/string this)))

(defrecord SymbolNode [value string-value]
  node/Node
  (tag [_n] :token)
  (node-type [_n] :symbol)
  (printable-only? [_n] false)
  (sexpr* [_node _opts] value)
  (length [_n] (count string-value))
  (string [_n] string-value)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)
(node/make-printable! SymbolNode)

(defn symbol-node? [n]
  (= :symbol (node/node-type n)))

;; ## Constructor

(defn token-node
  "Create node for an unspecified token of `value`."
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
    (if (symbol? value)
      (->SymbolNode value string-value)
      (->TokenNode value string-value))))
