(ns rewrite-clj.zip.insert-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [insert :refer :all]]
            [clojure.zip :as z]))

(tabular
  (fact "about whitespace-aware insertion."
        (let [elements (->> (base/of-string
                              (format ?fmt "1 2 3 4"))
                            (iterate ?m))
              loc (nth elements ?n)
              loc' (?f loc 'x)]
          (base/tag loc) => (base/tag loc')
          (base/root-string loc') => ?s))
  ?fmt     ?m       ?n   ?f               ?s
  "[%s]"   m/next   0    insert-right     "[1 2 3 4] x"
  "[%s]"   m/next   1    insert-right     "[1 x 2 3 4]"
  "[%s]"   m/next   2    insert-right     "[1 2 x 3 4]"
  "[%s]"   m/next   3    insert-right     "[1 2 3 x 4]"
  "[%s]"   m/next   4    insert-right     "[1 2 3 4 x]"
  "[%s]"   m/next   0    insert-left      "x [1 2 3 4]"
  "[%s]"   m/next   1    insert-left      "[x 1 2 3 4]"
  "[%s]"   m/next   2    insert-left      "[1 x 2 3 4]"
  "[%s]"   m/next   3    insert-left      "[1 2 x 3 4]"
  "[%s]"   m/next   4    insert-left      "[1 2 3 x 4]"
  "[%s]"   m/next   0    insert-child     "[x 1 2 3 4]"
  "[%s]"   m/next   0    append-child     "[1 2 3 4 x]"
  "[ %s]"  m/next   0    insert-child     "[x 1 2 3 4]"
  "[%s ]"  m/next   0    append-child     "[1 2 3 4 x]"
  "[%s]"   z/next   2    insert-right     "[1 x 2 3 4]")

(tabular
  (fact "about different node types that allow insertion."
        (let [loc (-> (iterate m/down (base/of-string ?s))
                      (nth (inc ?depth))
                      m/right
                      (insert-left 'x)
                      (insert-right 'y))]
          (base/root-string loc) => ?result))
  ?s                ?depth           ?result
  "[1 2]"           0                "[1 x 2 y]"
  "(1 2)"           0                "(1 x 2 y)"
  "#{1 2}"          0                "#{1 x 2 y}"
  "#(1 2)"          0                "#(1 x 2 y)"
  "'(1 2)"          1                "'(1 x 2 y)"
  "#=(1 2)"         1                "#=(1 x 2 y)"
  "#_(1 2)"         1                "#_(1 x 2 y)"
  "@(f 2)"          1                "@(f x 2 y)")

(future-fact
  "about inserting after comment."
  (let [loc (-> (base/of-string "[1 2 3] ; this is a comment")
                (z/rightmost)
                (insert-right 'x))]
    (base/root-string loc) => "[1 2 3] ; this is a comment\nx"))
