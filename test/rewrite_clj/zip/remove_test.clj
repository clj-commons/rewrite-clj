(ns rewrite-clj.zip.remove-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [remove :as r]]))

(tabular
  (fact "about whitespace-aware removal."
        (let [elements (->> (base/of-string ?data)
                            (iterate m/next))
              loc (nth elements ?n)
              loc' (r/remove loc)]
          (base/root-string loc') => ?s))
  ?data          ?n   ?s
  "[1 2 3 4]"    0    ""
  "[1 2 3 4]"    1    "[2 3 4]"
  "[1 2 3 4]"    2    "[1 3 4]"
  "[1 2 3 4]"    3    "[1 2 4]"
  "[1 2 3 4]"    4    "[1 2 3]"
  "[ 1 2 3 4]"   1    "[2 3 4]"
  "[1 2 3 4 ]"   4    "[1 2 3]"
  "[1]"          1    "[]"
  "[   1   ]"    1    "[]"
  "[;; c\n1]"    1    "[;; c\n]"
  "[1\n;; c\n2]" 1    "[;; c\n2]"
  "[1\n;; c\n2]" 2    "[1\n;; c\n]")
