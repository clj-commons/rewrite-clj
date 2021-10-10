(ns ^:no-doc rewrite-clj.node.uneval
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord UnevalNode [children]
  node/Node
  (tag [_node] :uneval)
  (node-type [_node] :uneval)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [_node]
    (+ 2 (node/sum-lengths children)))
  (string [_node]
    (str "#_" (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (node/assert-single-sexpr children')
    (assoc node :children children'))
  (leader-length [_node] 2)

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! UnevalNode)

;; ## Constructor

(defn uneval-node
  "Create node representing an unevaled form with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/uneval-node [(n/spaces 1)
                       (n/token-node 42)])
       n/string)
   ;; => \"#_ 42\"
   ```"
  [children]
  (if (sequential? children)
    (do
      (node/assert-single-sexpr children)
      (->UnevalNode children))
    (recur [children])))
