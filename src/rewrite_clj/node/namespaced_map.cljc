(ns ^:no-doc rewrite-clj.node.namespaced-map
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

(defrecord MapQualifierNode [auto-resolved? prefix]
  node/Node
  (tag [_node] :map-qualifier)
  (node-type [_node] :map-qualifier)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (if auto-resolved?
      ((or (:auto-resolve opts) node/default-auto-resolve)
       (if prefix (symbol prefix) :current))
      (symbol prefix)))
  (length [_node]
    (+ 1 ;; for first :
       (if auto-resolved? 1 0) ;; for extra :
       (count prefix)))
  (string [_node]
    (str ":"
         (when auto-resolved? ":")
         prefix))

  Object
  (toString [node]
    (node/string node)))

(defn- edit-map-children
  "A map node's children are a list of nodes that can contain non-sexpr-able elements (ex. whitespace).

  Returns `children` with `f` applied sexpressable children.

  `f` is called with
  - `n` - node
  - `is-map-key?` true if the node is in keyword position
  and should return `n` or a new version of `n`."
  [children f]
  (loop [r children
         last-key nil
         new-children []]
    (if-let [n (first r)]
      (if (node/printable-only? n)
        (recur (rest r)
               last-key
               (conj new-children n))
        (if last-key
          (recur (rest r)
                 nil
                 (conj new-children (f n false)))
          (recur (rest r)
                 n
                 (conj new-children (f n true)))))
      new-children)))

(defn- apply-context-to-map
  "Apply the context of the qualified map to the keyword keys in the map.

  Strips context from keyword-nodes not in keyword position and adds context to keyword nodes in keyword position."
  [m-node q-node]
  (node/replace-children m-node
                         (edit-map-children (node/children m-node)
                                            (fn [n is-map-key?]
                                              (if (satisfies? node/MapQualifiable n)
                                                (if is-map-key?
                                                  (node/map-context-apply n q-node)
                                                  (node/map-context-clear n))
                                                n)))))

(defn- apply-context [children]
  (let [q-node (first children)
        m-node (last children)]
    (concat (drop-last children)
            [(apply-context-to-map m-node q-node)])))

(defn reapply-namespaced-map-context
  "Namespaced map qualifier context is automatically applied to keyword children of contained map automatically on:
  - [[node/namespaced-map-node]] creation (i.e. at parse time)
  - [[node/replace-children]]

  If you make changes outside these techniques, call this function to reapply the qualifier context.

  This is only necessary if you need `sexpr` on map keywords to reflect the namespaced map qualifier.

  Returns `n` if not a namespaced map node."
  [n]
  (if (= :namespaced-map (node/tag n))
    (node/replace-children n (apply-context (node/children n)))
    n))

(defn- namespaced-map-sexpr
  "Assumes that appropriate qualifier context has been applied to contained map."
  [children opts]
  (node/sexpr (last children) opts))

(defrecord NamespacedMapNode [children]
  node/Node
  (tag [_node] :namespaced-map)
  (node-type [_node] :namespaced-map)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (namespaced-map-sexpr children opts))
  (length [_node]
    (+ 1 ;; for leading #
       (node/sum-lengths children)))
  (string [_node]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (assoc node :children (apply-context children')))
  (leader-length [_node]
    (dec 2))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! MapQualifierNode)
(node/make-printable! NamespacedMapNode)

;; ## Constructors

(defn map-qualifier-node
  "Create a map qualifier node.
   The map qualifier node is a child node of [[namespaced-map-node]].

   ```Clojure
   (require '[rewrite-clj.node :as n])

   ;; qualified
   (-> (n/map-qualifier-node false \"my-prefix\")
       n/string)
   ;; => \":my-prefix\"

   ;; auto-resolved to current ns
   (-> (n/map-qualifier-node true nil)
       n/string)
   ;; => \"::\"

   ;; auto-resolve to namespace with alias
   (-> (n/map-qualifier-node true \"my-ns-alias\")
       n/string)
   ;; => \"::my-ns-alias\"
   ```"
  [auto-resolved? prefix]
  (->MapQualifierNode auto-resolved? prefix))

(defn namespaced-map-node
  "Create a namespaced map node with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/namespaced-map-node [(n/map-qualifier-node true \"my-ns-alias\")
                               (n/spaces 1)
                               (n/map-node [(n/keyword-node :a)
                                            (n/spaces 1)
                                            (n/token-node 1)])])
       n/string)
   ;; => \"#::my-ns-alias {:a 1}\"
   ```

   Map qualifier context is automatically applied to map keys for sexpr support.

   See also [[map-qualifier-node]] and [[map-node]]."
  [children]
  (->NamespacedMapNode (apply-context children)))
