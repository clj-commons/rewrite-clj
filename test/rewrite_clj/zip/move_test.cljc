(ns rewrite-clj.zip.move-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.zip :as z]))

(let [root (z/of-string "[ 1 [2 3]   4]")]
  (deftest t-whitespace-aware-movement
    (are [?ops ?sexpr ?pred]
         (let [loc ((apply comp (reverse ?ops)) root)]
           (is (= ?sexpr (z/sexpr loc)))
           (is (?pred loc)))
      [z/down]                          1           identity
      [z/next]                          1           (complement z/end?)
      [z/next z/next]                   [2 3]       (complement z/end?)
      [z/down z/right z/right]          4           (complement z/end?)
      [z/down z/rightmost]              4           z/rightmost?
      [z/down z/rightmost z/rightmost]  4           z/rightmost?
      [z/down z/leftmost]               1           z/leftmost?
      [z/down z/right z/leftmost]       1           z/leftmost?
      [z/down z/next z/next z/up]       [2 3]       identity
      [z/down z/next z/next z/up z/up]  [1 [2 3] 4] #(and (z/leftmost? %) (z/rightmost? %))
      [z/down z/rightmost z/prev]       3           identity
      [z/down z/rightmost z/next]       4           z/end?)))

(deftest t-moving-into-an-empty-inner-node
  (let [zloc (z/of-string "[]")]
    (is (nil? (z/down zloc)))))
