(ns rewrite-clj.node.seqs
  (:require [rewrite-clj.node.protocols :as node]))


(def ^:private nodes
  [{:tag           :vector
    :format-string "[%s]"
    :sexpr-expr    '(vec (node/sexprs children))}
   
   {:tag           :list
    :format-string "(%s)"
    :sexpr-expr    '(apply list (node/sexprs children))}
   
   {:tag           :set
    :format-string "#{%s}"
    :sexpr-expr    '(set (node/sexprs children))}
   
   {:tag           :map
    :format-string "{%s}"
    :sexpr-expr    '(apply hash-map (node/sexprs children))}])

(defn- record-sym [n]
  (-> n :tag name clojure.string/capitalize
      (str "Node")
      symbol))

(defn- constructor-sym [n]
  (-> n :tag name
      (str "-node")
      symbol))

(defmacro ^:private def-all-nodes []
  `(do ~@(for [n nodes]
           `(do
              (defrecord ~(record-sym n) [~'children]
                node/Node
                (~'tag [~'_]
                  ~(:tag n))
                (~'printable-only? [~'_]
                  false)
                (~'length [~'this]
                  (+ (.wrap-length ~'this)
                     (node/sum-lengths ~'children)))
                (~'string [~'this]
                  (->> (node/concat-strings ~'children)
                       (format (.format-string ~'this))))
                (~'sexpr [~'this]
                  ~(:sexpr-expr n))
                
                node/InnerNode
                (~'inner? [~'_]
                  true)
                (~'children [~'this]
                  (:children ~'this))
                (~'replace-children [~'this ~'children]
                  (assoc ~'this :children ~'children))
                (~'leader-length [~'this]
                  (dec (.wrap-length ~'this)))
                
                node/SeqNode
                (~'wrap-length [~'this]
                  (count (.wrap ~'this "")))
                (~'wrap [~'this ~'children-str]
                  (format (.format-string ~'this)
                           ~'children-str))
                (~'format-string [~'_]
                  ~(:format-string n))
                
                Object
                (~'toString [~'this]
                  (.string ~'this)))
              
              (node/make-printable! ~(record-sym n))
              
              ;; ## Constructor
              
              (defn ~(constructor-sym n)
                ~(format "Create a node representing an EDN %s."
                         (-> n :tag name))
                [children#]
                (~(symbol (str "->" (record-sym n))) children#))))))

(def-all-nodes)
