(ns rewrite-clj.node.predicates
  (:refer-clojure :exclude [seq? list? vector? set? map? keyword? string?
                            integer? fn?])
  (:require [rewrite-clj.node.protocols :as node])
  (:import [rewrite_clj.node.reader_macro ReaderNode]))

(def node-tags
  #{:list
    :vector
    :set
    :map
    :quote
    :uneval
    :token
    :integer
    :keyword
    :string
    :comment
    :whitespace
    :newline
    :comma
    :meta
    :deref
    :forms
    :reader-macro})

(declare reader?)

(defn node?
  "Checks whether the given object is a rewrite-clj node."
  [object]
  (or (contains? node-tags (node/tag object))
      (reader? object)))


(defn seq?
  "Checks whether the given node represents a seq"
  [node]
  (contains?
    #{:list
      :vector
      :set
      :map}
    (node/tag node)))

(defn list?
  "Checks whether the given node represents a list"
  [node]
  (= (node/tag node) :list))

(defn vector?
  "Checks whether the given node represents a vector"
  [node]
  (= (node/tag node) :vector))

(defn set?
  "Checks whether the given node represents a set"
  [node]
  (= (node/tag node) :set))

(defn map?
  "Checks whether the given node represents a map"
  [node]
  (= (node/tag node) :map))

(defn quote?
  "Checks whether the given node represents a quote"
  [node]
  (= (node/tag node) :quote))

(defn uneval?
  "Checks whether the given node represents an unevaled form"
  [node]
  (= (node/tag node) :uneval))

(defn token?
  "Checks whether the given node represents a token"
  [node]
  (= (node/tag node) :token))

(defn integer?
  "Checks whether the given node represents an integer"
  [node]
  (= (node/tag node) :integer))

(defn keyword?
  "Checks whether the given node represents a keyword"
  [node]
  (= (node/tag node) :keyword))

(defn string?
  "Checks whether the given node represents a string"
  [node]
  (= (node/tag node) :string))

(defn comment?
  "Checks whether the given node represents a comment"
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
  "Checks whether a node represents linebreaks."
  [node]
  (= (node/tag node) :newline))
(def newline? linebreak?)

(defn comma?
  "Checks whether a node represents a comma."
  [node]
  (= (node/tag node) :comma))

(defn whitespace-or-comment?
  "Checks whether the given node represents a whitespace or a comment."
  [node]
  (or (whitespace? node)
      (comment? node)))

(defn fn?
  "Checks whether the given node represents anonymous function."
  [node]
  (= (node/tag node) :fn))

(defn meta?
  "Checks whether the given node represents a metadata reader macro."
  [node]
  (= (node/tag node) :meta))

(defn regex?
  "Checks whether the given node represents a regex."
  [node]
  (= (node/tag node) :regex))

(defn deref?
  "Checks whether the given node represents the deref reader macro."
  [node]
  (= (node/tag node) :deref))

(defn forms?
  "Checks whether the given node represents forms."
  [node]
  (= (node/tag node) :forms))

(defn form?
  "Checks whether the given node represents a form"
  [node]
  (not (contains?
         #{:whitespace :newline :comma :comment :uneval}
         (node/tag node))))

(defn reader-macro?
  "Checks whether the given node represents a reader-macro."
  [node]
  (= (node/tag node) :reader-macro))

(defn reader?
  "Checks whether the given node represents a named reader-macro."
  [node]
  (isa? node ReaderNode))
