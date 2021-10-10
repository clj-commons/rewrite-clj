(ns ^:no-doc rewrite-clj.node.forms
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord FormsNode [children]
  node/Node
  (tag [_node] :forms)
  (node-type [_node] :forms)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (let [es (node/sexprs children opts)]
      (if (next es)
        (list* 'do es)
        (first es))))
  (length [_node]
    (node/sum-lengths children))
  (string [_node]
    (node/concat-strings children))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_node] 0)

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! FormsNode)

;; ## Constructor

(defn forms-node
  "Create top-level node wrapping multiple `children`.
   The forms node is equivalent to an implicit `do` at the top-level.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/forms-node [(n/token-node 1)
                      (n/spaces 1)
                      (n/token-node 2)])
       n/string)
   ;; => \"1 2\"
   ```
   "
  [children]
  (->FormsNode children))
