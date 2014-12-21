(ns rewrite-clj.node.coerce-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node.protocols
             :as node :refer [coerce]]
            [rewrite-clj.node.coerce :refer :all]))

(tabular
  (fact "about sexpr -> node -> sexpr roundtrip."
        (let [n (coerce ?sexpr)]
          n => #(satisfies? node/Node %)
          (node/string n) => string?
          (node/sexpr n) => ?sexpr
          (class (node/sexpr n)) => (class ?sexpr)))
  ?sexpr
  ;; numbers
  3
  3N
  3.14
  3.14M
  3e14
  3/4

  ;; symbol/keyword/string/...
  'symbol
  'namespace/symbol
  :keyword
  :1.5.1
  ::keyword
  ::1.5.1
  :namespace/keyword
  ""
  "hello, over there!"
  #"abc"

  ;; seqs
  []
  [1 2 3]
  '()
  '(1 2 3)
  #{}
  #{1 2 3}

  ;; date
  #inst "2014-11-26T00:05:23"

  ;; map
  (hash-map)
  (hash-map :a 0, :b 1))

(fact "about vars."
      (let [n (coerce #'identity)]
        n => #(satisfies? node/Node %)
        (node/sexpr n) => '(var clojure.core/identity)))
