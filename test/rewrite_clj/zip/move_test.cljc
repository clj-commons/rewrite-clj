(ns rewrite-clj.zip.move-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]))

(let [root (base/of-string "[ 1 [2 3]   4]")]
  (deftest t-whitespace-aware-movement
    (are [?ops ?sexpr ?pred]
         (let [loc ((apply comp (reverse ?ops)) root)]
           (is (= ?sexpr (base/sexpr loc)))
           (is (?pred loc)))
      [m/down]                          1           identity
      [m/next]                          1           (complement m/end?)
      [m/next m/next]                   [2 3]       (complement m/end?)
      [m/down m/right m/right]          4           (complement m/end?)
      [m/down m/rightmost]              4           m/rightmost?
      [m/down m/rightmost m/rightmost]  4           m/rightmost?
      [m/down m/leftmost]               1           m/leftmost?
      [m/down m/right m/leftmost]       1           m/leftmost?
      [m/down m/next m/next m/up]       [2 3]       identity
      [m/down m/next m/next m/up m/up]  [1 [2 3] 4] #(and (m/leftmost? %) (m/rightmost? %))
      [m/down m/rightmost m/prev]       3           identity
      [m/down m/rightmost m/next]       4           m/end?)))

(deftest t-moving-into-an-empty-inner-node
  (let [zloc (base/of-string "[]")]
    (is (nil? (m/down zloc)))))
