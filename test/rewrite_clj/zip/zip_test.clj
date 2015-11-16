(ns rewrite-clj.zip.zip-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.test-helpers :refer :all]
            [rewrite-clj.zip.zip :as z]))

(fact "zipper starts with position [1 1]"
  (z/position (z/zipper (node/comment-node "hello"))) => [1 1])

(tabular
  (fact "z/down computes position correctly"
    (-> (z/zipper (?type [(node/token-node "hello")]))
      z/down
      z/position) => ?pos)
  ?type            ?pos
  node/forms-node  [1 1]
  node/fn-node     [1 3]
  node/quote-node  [1 2])
