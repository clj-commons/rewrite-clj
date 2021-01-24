(ns ^:no-doc rewrite-clj.node.regex
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord RegexNode [pattern]
  rewrite-clj.node.protocols/Node
  (tag [_node] :regex)
  (node-type [_node] :regex)
  (printable-only? [_node] false)
  (sexpr* [_node _opts]
    (list 're-pattern pattern))
  (length [_node] 1)
  (string [_node] 
    (str "#\"" pattern "\""))

  ;; TODO: interesting, no toString like other nodes 
  )

(node/make-printable! RegexNode)

;; ## Constructor

(defn regex-node
  "Create node representing a regex with `pattern-string`"
  [pattern-string]
  (->RegexNode pattern-string))
