(ns ^:no-doc rewrite-clj.node.stringz
  (:require [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [rewrite-clj.node.protocols :as node] ))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- wrap-string [s]
  (str "\"" s "\""))

(defn- join-lines [lines]
  (string/join "\n" lines))

(defrecord StringNode [lines]
  node/Node
  (tag [_node]
    (if (next lines)
      :multi-line
      :token))
  (node-type [_node] :string)
  (printable-only? [_node] false)
  (sexpr* [_node _opts]
    (join-lines
      (map
        (comp edn/read-string wrap-string)
        lines)))
  (length [_node]
    (+ 2 (reduce + (map count lines))))
  (string [_node]
    (wrap-string (join-lines lines)))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! StringNode)

;; ## Constructors

(defn string-node
  "Create node representing a string value where `lines`
   can be a sequence of strings or a single string."
  [lines]
  (if (string? lines)
    (->StringNode [lines])
    (->StringNode lines)))
