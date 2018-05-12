(ns rewrite-clj.zip.insert-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [insert :refer :all]]
            [rewrite-clj.custom-zipper.core :as z]))

(deftest t-whitespace-aware-insertion
  (are [?fmt ?m ?n ?f ?s]
       (let [elements (->> (base/of-string
                            (format ?fmt "1 2 3 4"))
                           (iterate ?m))
             loc (nth elements ?n)
             loc' (?f loc 'x)]
         (is (= (base/tag loc') (base/tag loc)))
         (is (= ?s (base/root-string loc'))))
    "[%s]"   m/next     0    insert-right     "[1 2 3 4] x"
    "[%s]"   m/next     1    insert-right     "[1 x 2 3 4]"
    "[%s]"   m/next     2    insert-right     "[1 2 x 3 4]"
    "[%s]"   m/next     3    insert-right     "[1 2 3 x 4]"
    "[%s]"   m/next     4    insert-right     "[1 2 3 4 x]"
    "[%s]"   m/next     0    insert-left      "x [1 2 3 4]"
    "[%s]"   m/next     1    insert-left      "[x 1 2 3 4]"
    "[%s]"   m/next     2    insert-left      "[1 x 2 3 4]"
    "[%s]"   m/next     3    insert-left      "[1 2 x 3 4]"
    "[%s]"   m/next     4    insert-left      "[1 2 3 x 4]"
    "[%s]"   m/next     0    insert-child     "[x 1 2 3 4]"
    "[%s]"   m/next     0    append-child     "[1 2 3 4 x]"
    "[ %s]"  m/next     0    insert-child     "[x 1 2 3 4]"
    "[%s ]"  m/next     0    append-child     "[1 2 3 4 x]"
    "[%s]"   z/next     2    insert-right     "[1 x 2 3 4]"
    "\n[%s]" z/leftmost 1    insert-left      "x\n[1 2 3 4]"
    "\n[%s]" z/leftmost 1    insert-right     "\nx [1 2 3 4]"))

(deftest t-different-node-types-that-allow-insertion
  (are [?s ?depth ?result]
       (let [loc (-> (iterate m/down (base/of-string ?s))
                     (nth (inc ?depth))
                     m/right
                     (insert-left 'x)
                     (insert-right 'y))]
         (is (= ?result (base/root-string loc))))
    "[1 2]"           0                "[1 x 2 y]"
    "(1 2)"           0                "(1 x 2 y)"
    "#{1 2}"          0                "#{1 x 2 y}"
    "#(1 2)"          0                "#(1 x 2 y)"
    "'(1 2)"          1                "'(1 x 2 y)"
    "#=(1 2)"         1                "#=(1 x 2 y)"
    "#_(1 2)"         1                "#_(1 x 2 y)"
    "@(f 2)"          1                "@(f x 2 y)"))

(comment
  ;; future-fact
  (deftest t-inserting-after-comment
    (let [loc (-> (base/of-string "[1 2 3] ; this is a comment")
                  (z/rightmost)
                  (insert-right 'x))]
      (is (= "[1 2 3] ; this is a comment\nx" (base/root-string loc))))))
