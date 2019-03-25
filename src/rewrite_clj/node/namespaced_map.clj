(ns ^:no-doc rewrite-clj.node.namespaced-map
  (:require
   [rewrite-clj.node.protocols :as node]))

;; ## Node

(defn- assert-namespaced-map-children
  [children]
  (let [exs (node/sexprs children)]
    (assert (= (count exs) 2)
            "must contain 2 non-whitespace forms.")
    (assert (keyword? (first exs))
            "first form in namespaced map must be a keyword.")
    (assert (map? (second exs))
            "second form in namespaced map must be a map.")))

(defrecord NamespacedMapNode [children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    (let [[nspace-keyword m] (node/sexprs children)
          nspace (str nspace-keyword)
          resolved-nspace (if (= "::" nspace)
                            (str (ns-name *ns*))
                            (if (.startsWith nspace "::")
                              (some-> (ns-aliases *ns*)
                                      (get (symbol (subs nspace 2)))
                                      (ns-name)
                                      str)
                              (subs nspace 1)))]
      (assert resolved-nspace
              (str ":namespaced-map could not resolve namespace " nspace-keyword))
      (->> (for [[k v] m
                 :let [k' (cond (not (keyword? k))     k
                                (= (namespace k) "_")  (keyword (name k))
                                (namespace k)          k
                                :else (keyword resolved-nspace (name k)))]]
             [k' v])
          (into {}))))
  (length [_]
    (+ 1 (node/sum-lengths children)))
  (string [this]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_]
    children)
  (replace-children [this children']
    (assert-namespaced-map-children children')
    (assoc this :children children'))
  (leader-length [_]
    1)

  Object
  (toString [this]node/string this))

(node/make-printable! NamespacedMapNode)

;; ## Constructors

(defn namespaced-map-node
  "Create a node representing a namespaced map.

  `#:prefix{a: 1}`  prefix namespaced map
  `#::{a: 1}`       auto-resolve namespaced map
  `#::alias{a: 1}`  auto-resolve alias namespaced map

  First arg is delivered as token node with keyword, second arg is map node.
  When first arg is :: keyword can be contrived via (keyword \":\")"
  [children]
  (assert-namespaced-map-children children)
  (->NamespacedMapNode children))
