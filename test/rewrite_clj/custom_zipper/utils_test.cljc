(ns rewrite-clj.custom-zipper.utils-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]))

(let [a (node/token-node 'a)
      b (node/token-node 'b)
      c (node/token-node 'c)
      d (node/token-node 'd)
      loc (z/down (base/edn* (node/forms-node [a b c d])))]
  (deftest t-remove-right
    (let [loc' (u/remove-right loc)]
      (is (= 'a (base/sexpr loc')))
      (is (= "acd" (base/root-string loc')))))
  (deftest t-remove-left
    (let [loc' (-> loc z/right z/right u/remove-left)]
      (is (= 'c (base/sexpr loc')))
      (is (= "acd" (base/root-string loc')))))
  (deftest t-remove-and-move-right
    (let [loc' (u/remove-and-move-right (z/right loc))]
      (is (= 'c (base/sexpr loc')))
      (is (= "acd" (base/root-string loc')))))
  (deftest t-remove-and-move-left
    (let [loc' (-> loc z/right u/remove-and-move-left)]
      (is (= 'a (base/sexpr loc')))
      (is (= "acd" (base/root-string loc'))))))

(deftest t-remove-and-move-left-tracks-current-position-correctly
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-and-move-left zloc)))))
    3  [1 3]
    5  [1 6]
    2  [1 2]))

(deftest t-remove-and-move-right-does-not-affect-position
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-and-move-right zloc)))))
    3  [1 4]
    1  [1 2]
    2  [1 3]))

(deftest t-remove-left-tracks-current-position-correctly
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-left zloc)))))
    3  [1 3]
    5  [1 6]))
