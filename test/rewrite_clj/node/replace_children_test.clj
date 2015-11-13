(ns rewrite-clj.node.replace-children-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :refer :all]))

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

(tabular
  (facts "replace-children fixes child positions"
    (let [node (with-positions (?ctor []) ?pos)
          children (map
                     #(with-positions (token-node "foo") %)
                     ?children)
          node (replace-children node children)]
      (positions node) => ?result))
    ?ctor      ?pos      ?children             ?result
    forms-node [1 1 1 5] [[1 1 1 3]]           [1 1 1 3]
    forms-node [1 1 1 5] [[1 1 1 3] [1 1 1 3]] [1 1 1 5])
