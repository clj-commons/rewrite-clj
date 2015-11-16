(ns rewrite-clj.zip.zip-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.test-helpers :refer :all]
            [rewrite-clj.zip.zip :as z]))

(fact "zipper starts with position [1 1]"
  (z/position (z/zipper (node/comment-node "hello"))) => [1 1])
