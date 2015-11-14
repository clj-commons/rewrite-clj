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

(def atom-node
  (gen/one-of [integer-node
               keyword-node
               string-node
               token-node]))

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
  [;ctor               min max
   [#'node/forms-node  0   5]
   [#'node/list-node   0   5]
   [#'node/map-node    0   5]
   [#'node/quote-node  1   1]
   [#'node/set-node    0   5]
   [#'node/var-node    1   1]
   [#'node/vector-node 0   5]])

(defn- container*
  [child-generator [ctor min max]]
  (gen/fmap ctor (gen/vector child-generator min max)))

(def node
  (gen/recursive-gen 
    (fn [inner]
      (gen/one-of
        (map
          (partial container* inner)
          containers)))
    atom-node))

(def children
  (gen/vector node))
