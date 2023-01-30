(ns rewrite-clj.node.equals-test
  (:require
   [clojure.test :refer [deftest is]]
   [rewrite-clj.node :as node]))

(deftest equals-test
  (is (= (node/list-node []) (node/list-node [])))
  (is (= (node/vector-node []) (node/vector-node [])))
  (is (= (node/set-node []) (node/set-node [])))
  (is (= (node/map-node []) (node/map-node []))))
