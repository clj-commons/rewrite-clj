(ns ^:no-doc rewrite-clj.node.invalid
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord InvalidKeywordNode [double-colon? ?ns sep? ?name]
  node/Node
  (tag [_node] :token)
  (node-type [_node] :invalid-keyword)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [node]
    (count (node/string node)))
  (string [_node]
    (str ":" (when double-colon? ":") ?ns (when sep? "/") ?name))

  Object
  (toString [node]
    (node/string node)))

(defrecord InvalidSymbolNode [?ns sep? ?name]
  node/Node
  (tag [_node] :token)
  (node-type [_node] :invalid-symbol)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [node]
    (count (node/string node)))
  (string [_node]
    (str ?ns (when sep? "/") ?name))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! InvalidKeywordNode)
(node/make-printable! InvalidSymbolNode)

;; ## Constructors

(defn invalid-keyword-node
  "Create a node representing an invalid keyword, a colon with an optional second colon indicating auto-resolved, an optional string namespace, optional boolean separator, and optional string name.

  The created nodes are printable only, even if they are technically conforming valid keywords, and the printed string won't necessarily look invalid.

  Note: If given both namespace and name, separator will be set to `true` (not possible to read an invalid keyword that has both namespace and name without a slash separator).

  ```clojure
  (require '[rewrite-clj.node :as n])

  ;; nothing (a bare `:`)
  (-> (n/invalid-keyword-node)
      n/string)
  ;; => \":\"

  ;; namespace or name (separator is true if both are provided)
  (-> (n/invalid-keyword-node \"front\" nil)
      n/string)
  ;; => \":front\"

  (-> (n/invalid-keyword-node nil \"back/double/triple\")
      n/string)
  ;; => \":back/double/triple\"

  (-> (n/invalid-keyword-node \"front\" \"back/double/triple\")
      n/string)
  ;; => \":front/back/double/triple\"

  ;; namespace _or_ name with deliberate separator
  (-> (n/invalid-keyword-node \"front\" true nil)
      n/string)
  ;; => \":front/\"

  (-> (n/invalid-keyword-node nil true \"back\")
      n/string)
  ;; => \":/back\"

  ;; double-colon
  (-> (n/invalid-keyword-node true nil true \"back\")
      n/string)
  ;; => \"::/back\"

  (-> (n/invalid-keyword-node true \"front\" true \"back\")
      n/string)
  ;; => \"::front/back\"
  ```"
  ([] (invalid-keyword-node false nil false nil))
  ([?ns ?name] (invalid-keyword-node false ?ns (and ?ns ?name) ?name))
  ([?ns sep? ?name] (invalid-keyword-node false ?ns sep? ?name))
  ([double-colon? ?ns sep? ?name]
   (->InvalidKeywordNode (boolean double-colon?)
                         ?ns
                         (if (and ?ns ?name)
                           true
                           (boolean sep?))
                         ?name)))

(defn invalid-symbol-node
  "Create a node representing an invalid symbol, a token with .

  The created nodes are printable only, even if they are technically conforming valid symbols, and the printed string won't necessarily look invalid.

  Note: If given both namespace and name, separator will be set to `true` (not possible to read an invalid symbol that has both namespace and name without a slash separator).

  ```clojure
  (require '[rewrite-clj.node :as n])

  ;; Unlike invalid-keyword-node, there's only one arity.
  ;; Making an invalid symbol is easier because it has more constraints.

  (-> (invalid-symbol-node \"a\" nil)
      (n/string))
  ;; => \"a/\"

  (-> (invalid-symbol-node nil \"b\")
      (n/string))
  ;; => \"/b\"

  (-> (invalid-symbol-node \"a\" \"///b\")
      (n/string))
  ;; => \"a////b\"
  ```"
  [?ns ?name]
  (->InvalidSymbolNode ?ns (or ?ns ?name) ?name))

(defn invalid-node?
  "Returns true if `n` is a node representing an invalid keyword or invalid symbol."
  [n]
  (let [nt (node/node-type n)]
    (or (= :invalid-keyword nt)
        (= :invalid-symbol nt))))
