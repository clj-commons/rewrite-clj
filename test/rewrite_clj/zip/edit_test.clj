(ns rewrite-clj.zip.edit-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [edit :as e]]
            [rewrite-clj.node :as node]))

(let [root (base/of-string "[1 \"2\" :3]")
      elements (iterate m/next root)]
  (deftest t-edit-operations
    (are [?n ?f ?s]
         (let [loc (nth elements ?n)
               loc' (e/edit loc #(-> % ?f))]
           (is (= ?s (base/root-string loc'))))
      0  (subvec 1)    "[\"2\" :3]"
      1  (str "_x")    "[\"1_x\" \"2\" :3]"
      2  (keyword "k") "[1 :2/k :3]"))
  (deftest t-replace-operations
    (are [?n ?v ?s]
         (let [loc (nth elements ?n)
               loc' (e/replace loc ?v)]
           (is (= ?s (base/root-string loc'))))
      0  [1]           "[1]"
      1  #{0}          "[#{0} \"2\" :3]")))

(let [root (base/of-string "[1 [ ] [2 3] [  4  ]]")
      elements (iterate m/next root)]
  (deftest t-splice-operations
    (are [?n ?s ?e]
         (let [loc (nth elements ?n)
               loc' (e/splice loc)]
           (is (= ?s (base/root-string loc')))
           (is (= ?e (base/sexpr loc'))))
      0  "1 [ ] [2 3] [  4  ]"    1
      1  "[1 [ ] [2 3] [  4  ]]"  1
      2  "[1 [2 3] [  4  ]]"      1
      3  "[1 [ ] 2 3 [  4  ]]"    2
      6  "[1 [ ] [2 3] 4]"        4)))

(deftest t-splicing-with-comment
  (are [?data ?s]
       (let [v (base/of-string ?data)
             loc (-> v m/down m/right e/splice)]
         (is (= ?s (base/root-string loc))))
    "[1 [2\n;; comment\n3]]"    "[1 2\n;; comment\n3]"
    "[1 [;;comment\n3]]"        "[1 ;;comment\n3]"
    "[1 [;;comment\n]]"         "[1 ;;comment\n]"
    "[1 [2\n;;comment\n]]"      "[1 2\n;;comment\n]"))

(deftest t-replacement-using-a-hand-crafted-node
  (are [?node ?s]
       (let [root (base/of-string "[1 2 3]")]
         (is (= ?s
                (-> root
                    m/next
                    (e/replace ?node)
                    base/root-string))))
    (node/token-node 255 "16rff")     "[16rff 2 3]"
    (node/integer-node 255 16)        "[0xff 2 3]"
    (node/integer-node 255 8)         "[0377 2 3]"
    (node/integer-node 9   2)         "[2r1001 2 3]"))
