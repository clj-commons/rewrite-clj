(ns rewrite-clj.custom-zipper.utils-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.custom-zipper.utils :refer :all]))

(let [a (node/token-node 'a)
      b (node/token-node 'b)
      c (node/token-node 'c)
      d (node/token-node 'd)
      loc (z/down (base/edn* (node/forms-node [a b c d])))]
  (fact "about 'remove-right'."
        (let [loc' (remove-right loc)]
          (base/sexpr loc') => 'a
          (base/root-string loc') => "acd"))
  (fact "about 'remove-left'."
        (let [loc' (-> loc z/right z/right remove-left)]
          (base/sexpr loc') => 'c
          (base/root-string loc') => "acd"))
  (fact "about 'remove-and-move-right'."
        (let [loc' (remove-and-move-right (z/right loc))]
          (base/sexpr loc') => 'c
          (base/root-string loc') => "acd"))
  (fact "about 'remove-and-move-left'."
        (let [loc' (-> loc z/right remove-and-move-left)]
          (base/sexpr loc') => 'a
          (base/root-string loc') => "acd")))

(tabular
  (fact "`remove-and-move-left` tracks current position correctly"
    (z/with-positional-zipper
      (let [root (base/of-string "[a bb ccc]")
            zloc (nth (iterate z/next root) ?n)]
        (z/position (remove-and-move-left zloc)) => ?pos)))
  ?n ?pos
  3  [1 3]
  5  [1 6]
  2  [1 2])

(tabular
  (fact "`remove-and-move-right` does not affect position"
    (z/with-positional-zipper
      (let [root (base/of-string "[a bb ccc]")
            zloc (nth (iterate z/next root) ?n)]
        (z/position (remove-and-move-right zloc)) => ?pos)))
  ?n ?pos
  3  [1 4]
  1  [1 2]
  2  [1 3])

(tabular
  (fact "`remove-left` tracks current position correctly"
    (z/with-positional-zipper
      (let [root (base/of-string "[a bb ccc]")
            zloc (nth (iterate z/next root) ?n)]
        (z/position (remove-left zloc)) => ?pos)))
  ?n ?pos
  3  [1 3]
  5  [1 6])
