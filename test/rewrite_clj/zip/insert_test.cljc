(ns rewrite-clj.zip.insert-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.interop :as interop]
            [rewrite-clj.zip :as z]))

(deftest t-whitespace-aware-insertion
  (are [?fmt ?m ?n ?f ?s]
       (let [elements (->> (z/of-string
                            (interop/simple-format ?fmt "1 2 3 4"))
                           (iterate ?m))
             loc (nth elements ?n)
             loc' (?f loc 'x)]
         (is (= (z/tag loc') (z/tag loc)))
         (is (= ?s (z/root-string loc'))))
    "[%s]"   z/next      0    z/insert-right     "[1 2 3 4] x"
    "[%s]"   z/next      1    z/insert-right     "[1 x 2 3 4]"
    "[%s]"   z/next      2    z/insert-right     "[1 2 x 3 4]"
    "[%s]"   z/next      3    z/insert-right     "[1 2 3 x 4]"
    "[%s]"   z/next      4    z/insert-right     "[1 2 3 4 x]"
    "[%s]"   z/next      0    z/insert-left      "x [1 2 3 4]"
    "[%s]"   z/next      1    z/insert-left      "[x 1 2 3 4]"
    "[%s]"   z/next      2    z/insert-left      "[1 x 2 3 4]"
    "[%s]"   z/next      3    z/insert-left      "[1 2 x 3 4]"
    "[%s]"   z/next      4    z/insert-left      "[1 2 3 x 4]"
    "[%s]"   z/next      0    z/insert-child     "[x 1 2 3 4]"
    "[%s]"   z/next      0    z/append-child     "[1 2 3 4 x]"
    "[ %s]"  z/next      0    z/insert-child     "[x 1 2 3 4]"
    "[%s ]"  z/next      0    z/append-child     "[1 2 3 4 x]"
    "[%s]"   z/next*     2    z/insert-right     "[1 x 2 3 4]"
    "\n[%s]" z/leftmost* 1    z/insert-left      "x\n[1 2 3 4]"
    "\n[%s]" z/leftmost* 1    z/insert-right     "\nx [1 2 3 4]"))

(deftest t-different-node-types-that-allow-insertion
  (are [?s ?depth ?result]
       (let [loc (-> (iterate z/down (z/of-string ?s))
                     (nth (inc ?depth))
                     z/right
                     (z/insert-left 'x)
                     (z/insert-right 'y))]
         (is (= ?result (z/root-string loc))))
    "[1 2]"           0                "[1 x 2 y]"
    "(1 2)"           0                "(1 x 2 y)"
    "#{1 2}"          0                "#{1 x 2 y}"
    "#(1 2)"          0                "#(1 x 2 y)"
    "'(1 2)"          1                "'(1 x 2 y)"
    "#=(1 2)"         1                "#=(1 x 2 y)"
    "#_(1 2)"         1                "#_(1 x 2 y)"
    "@(f 2)"          1                "@(f x 2 y)"))
