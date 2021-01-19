(ns rewrite-clj.node.integer-test
  (:require [clojure.test.check.properties :as prop]
            [clojure.test :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [clojure.test.check.clojure-test :refer :all]))

(defspec t-all-integer-nodes-produce-readable-strings 100
  (prop/for-all [node g/integer-node]
                (read-string (node/string node))))
