(ns rewrite-clj.zip.move-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]]))

(let [root (base/of-string "[ 1 [2 3]   4]")]
  (tabular
    (fact "about whitespace-aware movement."
          (let [loc ((apply comp (reverse ?ops)) root)]
            (base/sexpr loc) => ?sexpr
            loc => ?pred))
    ?ops                              ?sexpr      ?pred
    [m/down]                          1           truthy
    [m/next]                          1           (complement m/end?)
    [m/next m/next]                   [2 3]       (complement m/end?)
    [m/down m/right m/right]          4           (complement m/end?)
    [m/down m/rightmost]              4           m/rightmost?
    [m/down m/rightmost m/rightmost]  4           m/rightmost?
    [m/down m/leftmost]               1           m/leftmost?
    [m/down m/right m/leftmost]       1           m/leftmost?
    [m/down m/next m/next m/up]       [2 3]       truthy
    [m/down m/next m/next m/up m/up]  [1 [2 3] 4] #(and (m/leftmost? %) (m/rightmost? %))
    [m/down m/rightmost m/prev]       3           truthy
    [m/down m/rightmost m/next]       4           m/end?))

(fact "about moving into an empty inner node."
      (let [zloc (base/of-string "[]")]
        (-> zloc m/down m/up) => zloc))
