(ns rewrite-clj.node.predicates
  (:refer-clojure :exclude [seq? list? vector? set? map? keyword? string?])
  (:require [rewrite-clj.node.protocols :as node]))

(def node-tags
  #{:list
    :vector
    :set
    :map
    :quote
    :uneval
    :newline
    :token
    :keyword
    :string
    :comment
    :comma
    :whitespace})

(defn node?
  "Check whether the given node represents a node"
  [node]
  (contains? node-tags (node/tag node)))

(defn seq?
  "Check whether the given node represents a seq"
  [node]
  (contains?
    #{:list
      :vector
      :set
      :map}
    (node/tag node)))

(defn list?
  "Check whether the given node represents a list"
  [node]
  (= (node/tag node) :list))

(defn vector?
  "Check whether the given node represents a vector"
  [node]
  (= (node/tag node) :vector))

(defn set?
  "Check whether the given node represents a set"
  [node]
  (= (node/tag node) :set))

(defn map?
  "Check whether the given node represents a map"
  [node]
  (= (node/tag node) :map))

(defn quote?
  "Check whether the given node represents a quote"
  [node]
  (= (node/tag node) :quote))

(defn uneval?
  "Check whether the given node represents an unevaled form"
  [node]
  (= (node/tag node) :uneval))

(defn token?
  "Check whether the given node represents a token"
  [node]
  (= (node/tag node) :token))

(defn keyword?
  "Check whether the given node represents a keyword"
  [node]
  (= (node/tag node) :keyword))

(defn string?
  "Check whether the given node represents a string"
  [node]
  (= (node/tag node) :string))

(defn comment?
  "Check whether the given node represents a comment"
  [node]
  (= (node/tag node) :comment))

(defn whitespace?
  "Check whether a node represents whitespace."
  [node]
  (contains?
   #{:whitespace
     :newline
     :comma}
   (node/tag node)))

(defn linebreak?
  "Check whether a node represents linebreaks."
  [node]
  (= (node/tag node) :newline))

(defn comma?
  "Check whether a node represents a comma."
  [node]
  (= (node/tag node) :comma))

(defn whitespace-or-comment?
  "Check whether the given node represents a whitespace or a comment."
  [node]
  (or (whitespace? node)
      (comment? node)))
