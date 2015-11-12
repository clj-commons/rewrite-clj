(ns rewrite-clj.zip.utils-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.zip :as z]
            [rewrite-clj.zip.utils :refer :all]))

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
