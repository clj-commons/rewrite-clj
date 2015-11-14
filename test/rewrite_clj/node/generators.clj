(ns rewrite-clj.node.generators
  (:require [clojure.test.check.generators :as gen]
            [rewrite-clj.node :as node]))

;; Leaf nodes

(def comment-node
  (gen/fmap
    (fn [[text eol]]
      (node/comment-node (str text eol)))
    (gen/tuple
      (gen/such-that
        #(re-matches #"[^\r\n]*" %)
        gen/string-ascii)
      (gen/elements ["" "\r" "\n"]))))

(def integer-node
  (gen/fmap
    (fn [[n base]]
      (node/integer-node n base))
    (gen/tuple
      (gen/choose Long/MIN_VALUE Long/MAX_VALUE)
      (gen/choose 2 36))))

(def keyword-node
  (gen/fmap
    (fn [[kw namespaced?]]
      (node/keyword-node kw namespaced?))
    (gen/tuple
      gen/keyword
      gen/boolean)))

(def newline-node
  (gen/fmap
    (comp node/newline-node (partial apply str))
    (gen/vector (gen/elements [\newline \return]) 1 5)))

(def string-node
  (gen/fmap node/string-node gen/string-ascii))

(def token-node
  (gen/fmap node/token-node gen/symbol))

(def whitespace-node
  (gen/fmap
    (comp node/whitespace-node (partial apply str))
    (gen/vector (gen/elements [\, \space \tab]) 1 5)))

(def leaf-node
  (gen/one-of [comment-node
               integer-node
               keyword-node
               newline-node
               string-node
               token-node
               whitespace-node]))

;; Container nodes

;;deref-node
;;eval-node
;;fn-node
;;meta-node
;;quote-node
;;raw-meta-node
;;reader-macro-node
;;syntax-quote-node
;;uneval-node
;;unquote-node
;;unquote-splicing-node
;;var-node

(def ^:private containers
  [node/forms-node
   node/list-node
   node/map-node
   node/set-node
   node/vector-node])

(defn- container-node*
  [child-generator]
  (fn [ctor]
    (gen/fmap ctor (gen/vector child-generator))))

(defn- container-node
  [inner-generator]
  (gen/one-of
    (map
      (container-node* inner-generator)
      containers)))

(def node
  (gen/recursive-gen container-node leaf-node))

(def children
  (gen/vector node))
