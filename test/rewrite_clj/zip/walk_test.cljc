(ns rewrite-clj.zip.walk-test
  (:require [clojure.test :refer [deftest is]]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.editz :as e]
            [rewrite-clj.zip.seqz :as sq]
            [rewrite-clj.zip.walk :as w]))

(deftest t-zipper-tree-prewalk
  (let [root (base/of-string  "[0 [1 2 3] 4]")
        not-vec? (complement sq/vector?)
        inc-node #(e/edit % inc)
        inc-node-p #(if (sq/vector? %) % (inc-node %))]
    (is (= "[0 [1 2 3] 4]"
           (-> root
               (w/prewalk identity)
               base/root-string)))
    (is (= "[1 [2 3 4] 5]"
           (-> root
               (w/prewalk inc-node-p)
               base/root-string)))
    (is (= "[1 [2 3 4] 5]"
           (-> root
               (w/prewalk not-vec? inc-node)
               base/root-string)))
    (is (= "[3 [4 5 6] 7]"
           (-> (iterate #(w/prewalk % not-vec? inc-node) root)
               (nth 3)
               base/root-string)))))

(deftest t-zipper-tree-postwalk
  (let [root (base/of-string "[0 [1 2 3] 4]")
        wrap-node #(e/edit % list 'x)
        wrap-node-p #(if (sq/vector? %) (wrap-node %) %)]
    (is (= "[0 [1 2 3] 4]"
           (-> root
               (w/postwalk identity)
               base/root-string)))
    (is (= "([0 ([1 2 3] x) 4] x)"
           (-> root
               (w/postwalk wrap-node-p)
               base/root-string)))
    (is (= "([0 ([1 2 3] x) 4] x)"
           (-> root
               (w/postwalk sq/vector? wrap-node)
               base/root-string)))))
