(ns ^:no-doc rewrite-clj.node.regex
  (:require [rewrite-clj.node.protocols :as node]))


;; ## Node

(defrecord RegexNode [pattern]
  rewrite-clj.node.protocols/Node
  (tag [_] :regex)
  (printable-only? [_] false)
  (sexpr [_] (list 're-pattern pattern))
  (length [_] 1)
  (string [_] (str "#\"" pattern "\"")))

(node/make-printable! RegexNode)

;; ## Constructor

(defn regex-node
  "Create node representing a regex"
  [pattern-string]
  (->RegexNode pattern-string))
