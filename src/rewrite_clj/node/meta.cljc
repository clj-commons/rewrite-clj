(ns ^:no-doc rewrite-clj.node.meta
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord MetaNode [tag prefix children]
  node/Node
  (tag [_node] tag)
  (node-type [_node] :meta)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (let [[mta data] (node/sexprs children opts)]
      (assert (interop/meta-available? data)
              (str "cannot attach metadata to: " (pr-str data)))
      (vary-meta data merge (if (map? mta) mta {mta true}))))
  (length [_node]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_node]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))
  (leader-length [_node]
    (count prefix))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! MetaNode)

;; ## Constructor

(defn meta-node
  "Create a node representing a form with metadata.

   When creating manually, you can specify `metadata` and `data` and spacing between the 2 elems will be included:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/meta-node (n/keyword-node :foo)
                    (n/vector-node [(n/token-node 1)]))
       n/string)
   ;; => \"^:foo [1]\"

   (-> (n/meta-node (n/map-node [:foo (n/spaces 1) 42])
                    (n/vector-node [(n/token-node 1)]))
       n/string)
   ;; => \"^{:foo 42} [1]\"
   ```
   When specifying a sequence of `children`, spacing is explicit:

   ```Clojure
   (-> (n/meta-node [(n/keyword-node :foo)
                     (n/spaces 1)
                     (n/vector-node [(n/token-node 1)])])
       n/string)
   ;; => \"^:foo [1]\"
   ```
   See also: [[raw-meta-node]]"
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta "^" children))
  ([metadata data]
   (meta-node [metadata (ws/spaces 1) data])))

(defn raw-meta-node
  "Create a node representing a form with metadata that renders to the reader syntax.

   When creating manually, you can specify `metadata` and `data` and spacing between the 2 elems will be included:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/raw-meta-node (n/keyword-node :foo)
                        (n/vector-node [(n/token-node 2)]))
        n/string)
   ;; => \"#^:foo [2]\"

   (-> (n/raw-meta-node (n/map-node [:foo (n/spaces 1) 42])
                        (n/vector-node [(n/token-node 2)]))
       n/string)
   ;; => \"#^{:foo 42} [2]\"
   ```
   When specifying a sequence of `children`, spacing is explicit:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/raw-meta-node [(n/keyword-node :foo)
                         (n/spaces 1)
                         (n/vector-node [(n/token-node 2)])])
       n/string)
   ;; => \"#^:foo [2]\"
   ```
   See also: [[meta-node]]"
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta* "#^" children))
  ([metadata data]
   (raw-meta-node [metadata (ws/spaces 1) data])))
