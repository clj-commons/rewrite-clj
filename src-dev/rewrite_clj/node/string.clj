(ns rewrite-clj.node.string
  (:require [rewrite-clj.node.protocols :as node]
            [clojure.tools.reader.edn :as edn]
            [clojure.string :as string]))

;; ## Node

(defn- wrap-string
  [s]
  (format "\"%s\"" s))

(defn- join-lines
  [lines]
  (string/join "\n" lines))

(defrecord StringNode [lines]
  node/Node
  (tag [_]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_]
    false)
  (sexpr [_]
    (join-lines
      (map
        (comp edn/read-string wrap-string)
        lines)))
  (string [_]
    (wrap-string (join-lines lines)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! StringNode)

;; ## Constructors

(defn string-node
  [lines]
  {:pre [(every? string? lines)]}
  (->StringNode lines))
