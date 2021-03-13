(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Nodes

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [_node] tag)
  (node-type [_node] :seq)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (seq-fn (node/sexprs children opts)))
  (length [_node]
    (+ wrap-length (node/sum-lengths children)))
  (string [_node]
    (->> (node/concat-strings children)
         (interop/simple-format format-string)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (assoc node :children children'))
  (leader-length [_node]
    (dec wrap-length))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! SeqNode)

;; ## Constructors

(defn list-node
  "Create a node representing a list with `children`.
   
   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/list-node [(n/token-node 1)
                     (n/spaces 1)
                     (n/token-node 2)
                     (n/spaces 1)
                     (n/token-node 3)])
       n/string)
   ;; => \"(1 2 3)\"
   ```"
  [children]
  (->SeqNode :list "(%s)" 2 #(apply list %) children))

(defn vector-node
  "Create a node representing a vector with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n]) 

   (-> (n/vector-node [(n/token-node 1)
                       (n/spaces 1)
                       (n/token-node 2)
                       (n/spaces 1)
                       (n/token-node 3)])
       n/string)
   ;; => \"[1 2 3]\"
   ```"
  [children]
  (->SeqNode :vector "[%s]" 2 vec children))

(defn set-node
  "Create a node representing a set with `children`.
   
   ```Clojure
   (require '[rewrite-clj.node :as n]) 

   (-> (n/set-node [(n/token-node 1)
                    (n/spaces 1)
                    (n/token-node 2)
                    (n/spaces 1)
                    (n/token-node 3)])
       n/string) 
   ;; => \"#{1 2 3}\"
   ```"
  [children]
  (->SeqNode :set "#{%s}" 3 set children))

(defn map-node
  "Create a node representing a map with `children`.
   ```Clojure
   (require '[rewrite-clj.node :as n]) 

   (-> (n/map-node [(n/keyword-node :a)
                    (n/spaces 1)
                    (n/token-node 1)
                    (n/spaces 1)
                    (n/keyword-node :b)
                    (n/spaces 1)
                    (n/token-node 2)])
       (n/string))
   ;; => \"{:a 1 :b 2}\" 
   ```

   Note that rewrite-clj allows unbalanced maps:
   ```Clojure
   (-> (n/map-node [(n/keyword-node :a)])
       (n/string))
   ;; => \"{:a}\"
   ```
   Note also that [[sexpr]] will fail on an unbalanced map."
  [children]
  (->SeqNode :map "{%s}" 2 #(apply hash-map %) children))
