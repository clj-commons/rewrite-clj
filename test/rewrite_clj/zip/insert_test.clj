(ns rewrite-clj.zip.insert-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [insert :refer :all]]
            [fast-zip.core :as z]))

(tabular
  (fact "about whitespace-aware insertion."
        (let [elements (->> (base/of-string
                              (format ?fmt "1 2 3 4"))
                            (iterate m/next))
              loc (nth elements ?n)
              loc' (?f loc 'x)]
          (base/tag loc) => (base/tag loc')
          (base/root-string loc') => ?s))
  ?fmt      ?n   ?f               ?s
  "[%s]"    0    insert-right     "[1 2 3 4] x"
  "[%s]"    1    insert-right     "[1 x 2 3 4]"
  "[%s]"    2    insert-right     "[1 2 x 3 4]"
  "[%s]"    3    insert-right     "[1 2 3 x 4]"
  "[%s]"    4    insert-right     "[1 2 3 4 x]"
  "[%s]"    0    insert-left      "x [1 2 3 4]"
  "[%s]"    1    insert-left      "[x 1 2 3 4]"
  "[%s]"    2    insert-left      "[1 x 2 3 4]"
  "[%s]"    3    insert-left      "[1 2 x 3 4]"
  "[%s]"    4    insert-left      "[1 2 3 x 4]"
  "[%s]"    0    insert-child     "[x 1 2 3 4]"
  "[%s]"    0    append-child     "[1 2 3 4 x]"
  "[ %s]"   0    insert-child     "[x 1 2 3 4]"
  "[%s ]"   0    append-child     "[1 2 3 4 x]")

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
