(ns rewrite-clj.node.protocols
  (:require [potemkin :refer [defprotocol+]]
            [clojure.string :as string]))

;; ## Node

(defprotocol+ Node
  "Protocol for EDN/Clojure nodes."
  (tag [_]
    "Keyword representing the type of the node.")
  (printable-only? [_]
    "Return true if the node cannot be converted to an s-expression
     element.")
  (sexpr [_]
    "Convert node to s-expression.")
  (length [_]
    "Get number of characters for the string version of this node.")
  (string [_]
    "Convert node to printable string."))

(extend-protocol Node
  Object
  (tag [_] :unknown)
  (printable-only? [_] false)
  (sexpr [this] this)
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexprs
  "Given a seq of nodes, convert those that represent s-expressions
   to the respective forms."
  [nodes]
  (->> nodes
       (remove printable-only?)
       (map sexpr)))

(defn sum-lengths
  "Sum up lengths of the given nodes."
  [nodes]
  (reduce + (map length nodes)))

(defn concat-strings
  "Convert nodes to strings and concatenate them."
  [nodes]
  (reduce str (map string nodes)))

;; ## Inner Node

(defprotocol+ InnerNode
  "Protocol for non-leaf EDN/Clojure nodes."
  (inner? [_]
    "Check whether the node can contain children.")
  (children [_]
    "Get child nodes.")
  (replace-children [_ children]
    "Replace the node's children."))

(extend-protocol InnerNode
  Object
  (inner? [_] false)
  (children [_]
    (throw (UnsupportedOperationException.)))
  (replace-children [_ _]
    (throw (UnsupportedOperationException.))))

(defn child-sexprs
  "Get all child s-expressions for the given node."
  [node]
  (if (inner? node)
    (sexprs (children node))))

;; ## Coerceable

(defprotocol+ NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [_]))

;; ## Print Helper

(defn node->string
  [node]
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (format "%n  %s%n"))
             (str " " n))]
    (format "<%s:%s>" (name (tag node)) n')))

(defmacro make-printable!
  [class]
  `(defmethod print-method ~class
     [node# w#]
     (.write w# (node->string node#))))

;; ## Helpers

(defn assert-sexpr-count
  [nodes c]
  (assert
    (= (count (sexprs nodes)) c)
    (format "can only contain %d non-whitespace form%s."
            c (if (= c 1) "" "s"))))

(defn assert-single-sexpr
  [nodes]
  (assert-sexpr-count nodes 1))
