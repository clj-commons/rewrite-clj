(ns ^:no-doc rewrite-clj.node.coerce
  (:require [rewrite-clj.potemkin :refer [defprotocol+]]
            [rewrite-clj.node
             comment forms integer keyword
             quote string uneval
             [meta :refer [meta-node]]
             [protocols :as node
              :refer [NodeCoerceable
                      coerce]]
             [reader-macro
              :refer [reader-macro-node var-node]]
             [colls :refer [vector-node list-node set-node map-node]]
             [token :refer [token-node]]
             [whitespace :as ws]])
  (:import [rewrite_clj.node.comment CommentNode]
           [rewrite_clj.node.forms FormsNode]
           [rewrite_clj.node.integer IntNode]
           [rewrite_clj.node.keyword KeywordNode]
           [rewrite_clj.node.meta MetaNode]
           [rewrite_clj.node.quote QuoteNode]
           [rewrite_clj.node.reader_macro
            ReaderNode ReaderMacroNode DerefNode]
           [rewrite_clj.node.colls VectorNode ListNode SetNode MapNode]
           [rewrite_clj.node.string StringNode]
           [rewrite_clj.node.token TokenNode]
           [rewrite_clj.node.uneval UnevalNode]
           [rewrite_clj.node.whitespace WhitespaceNode NewlineNode]))

;; ## Helpers

(defn- node-with-meta
  [node value]
  (if (instance? clojure.lang.IMeta value)
    (let [mta (meta value)]
      (if (empty? mta)
        node
        (meta-node (coerce mta) node)))
    node))

;; ## Tokens

(extend-protocol NodeCoerceable
  Object
  (coerce [v]
    (node-with-meta
      (token-node v)
      v)))

(extend-protocol NodeCoerceable
  nil
  (coerce [v]
    (token-node nil)))

;; ## Seqs

(defn- seq-node
  [f sq]
  (node-with-meta
    (->> (map coerce sq)
         (ws/space-separated)
         (vec)
         (f))
    sq))

(extend-protocol NodeCoerceable
  clojure.lang.IPersistentVector
  (coerce [sq]
    (seq-node vector-node sq))
  clojure.lang.IPersistentList
  (coerce [sq]
    (seq-node list-node sq))
  clojure.lang.IPersistentSet
  (coerce [sq]
    (seq-node set-node sq)))

;; ## Maps

(let [comma (ws/whitespace-nodes ", ")
      space (ws/whitespace-node " ")]
  (defn- map->children
    [m]
    (->> (mapcat
          (fn [[k v]]
            (list* (coerce k) space (coerce v) comma))
          m)
         (drop-last (count comma))
         (vec))))

(defn- record-node
  [m]
  (reader-macro-node
    [(token-node (symbol (.getName ^Class (class m))))
     (map-node (map->children m))]))

(defn- is-record?
  [v]
  (instance? clojure.lang.IRecord v))

(extend-protocol NodeCoerceable
  clojure.lang.IPersistentMap
  (coerce [m]
    (node-with-meta
      (if (is-record? m)
        (record-node m)
        (map-node (map->children m)))
      m)))

;; ## Vars

(extend-protocol NodeCoerceable
  clojure.lang.Var
  (coerce [v]
    (-> (str v)
        (subs 2)
        (symbol)
        (token-node)
        (vector)
        (var-node))))

;; ## Existing Nodes

(extend-protocol NodeCoerceable
  CommentNode     (coerce [v] v)
  FormsNode       (coerce [v] v)
  IntNode         (coerce [v] v)
  KeywordNode     (coerce [v] v)
  MetaNode        (coerce [v] v)
  QuoteNode       (coerce [v] v)
  ReaderNode      (coerce [v] v)
  ReaderMacroNode (coerce [v] v)
  DerefNode       (coerce [v] v)
  StringNode      (coerce [v] v)
  UnevalNode      (coerce [v] v)
  NewlineNode     (coerce [v] v)
  VectorNode      (coerce [v] v)
  ListNode        (coerce [v] v)
  SetNode         (coerce [v] v)
  MapNode         (coerce [v] v)
  TokenNode       (coerce [v] v)
  WhitespaceNode  (coerce [v] v))
