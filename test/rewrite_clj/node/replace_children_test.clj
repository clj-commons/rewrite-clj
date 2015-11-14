(ns rewrite-clj.node.replace-children-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [rewrite-clj.test-helpers :refer :all]))

(defn positions
  [node]
  (let [{:keys [row col next-row next-col]} (meta node)]
    [row col next-row next-col]))

(defn with-positions
  [node [row col next-row next-col]]
  (with-meta node {:row row
                   :col col
                   :next-row next-row
                   :next-col next-col}))


(facts "about replacing children"
  (facts "replace-children preserves the meaning of the operation"
    (property "replace-children does not alter the number of children" 50
      (prop/for-all [[node children] (gen/bind
                                       (gen/such-that node/inner? (g/node))
                                       (fn [node]
                                         (gen/tuple
                                           (gen/return node)
                                           (gen/vector
                                             (gen/such-that
                                               (complement node/printable-only?)
                                                (g/node))
                                             (count (remove node/printable-only? (node/children node)))))))]
        (= (count children)
           (count (node/children (node/replace-children node children))))))))
