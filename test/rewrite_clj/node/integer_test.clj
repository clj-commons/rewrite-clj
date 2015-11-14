(ns rewrite-clj.node.integer-test
  (:require [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [rewrite-clj.test-helpers :refer :all]))

(facts "about integer nodes"
  (property "all integer nodes produce readable strings" 100
    (prop/for-all [node g/integer-node]
      (read-string (node/string node)))))
