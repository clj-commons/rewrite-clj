(ns rewrite-clj.node.generators
  (:require [clojure.test.check.generators :as gen]
            [rewrite-clj.node :as node]))

(def comment-node
  (gen/fmap
    (fn [[text eol]]
      (node/comment-node (str text eol)))
    (gen/tuple
      (gen/such-that
        #(re-matches #"[^\r\n]*" %)
        gen/string-ascii)
      (gen/elements ["" "\r" "\n"]))))

;;fn-node

(defn forms-node*
  [children-generator]
  (gen/fmap node/forms-node children-generator))

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

;;meta-node
;;raw-meta-node
;;deref-node
;;eval-node
;;reader-macro-node
;;var-node
;;list-node
;;map-node
;;set-node
;;vector-node
;;string-node
;;quote-node
;;syntax-quote-node
;;unquote-node
;;unquote-splicing-node
;;token-node
;;uneval-node

(def newline-node
  (gen/fmap
    (comp node/newline-node (partial apply str))
    (gen/vector (gen/elements [\newline \return]) 1 5)))

(def whitespace-node
  (gen/fmap
    (comp node/whitespace-node (partial apply str))
    (gen/vector (gen/elements [\, \space \tab]) 1 5)))

(def leaf-node
  (gen/one-of [comment-node
               integer-node
               keyword-node
               newline-node
               whitespace-node]))

(defn container-node
  [inner-generator]
  (gen/one-of [(forms-node* inner-generator)]))

(def node
  (gen/recursive-gen container-node leaf-node))

(def children
  (gen/vector node))

(def forms-node
  (forms-node* children))
