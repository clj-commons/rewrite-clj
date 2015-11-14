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
                                       (gen/elements g/container-node-types)
                                       (fn [type]
                                         (gen/tuple
                                           (g/node #{type})
                                           (gen/fmap node/children (g/node #{type})))))]
        (= (count children)
           (count (node/children (node/replace-children node children))))))))
