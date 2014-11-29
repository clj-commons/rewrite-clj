(ns rewrite-clj.zip.edit-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [edit :as e]]))

(let [root (base/of-string "[1 \"2\" :3]")
      elements (iterate m/next root)]
  (tabular
    (fact "about edit operations."
          (let [loc (nth elements ?n)
                loc' (e/edit loc #(-> % ?f))]
            (base/root-string loc') => ?s))
    ?n ?f            ?s
    0  (subvec 1)    "[\"2\" :3]"
    1  (str "_x")    "[\"1_x\" \"2\" :3]"
    2  (keyword "k") "[1 :2/k :3]")
  (tabular
    (fact "about replace operations."
          (let [loc (nth elements ?n)
                loc' (e/replace loc ?v)]
            (base/root-string loc') => ?s))
    ?n ?v            ?s
    0  [1]           "[1]"
    1  #{0}          "[#{0} \"2\" :3]"))

(let [root (base/of-string "[1 [ ] [2 3] [  4  ]]")
      elements (iterate m/next root)]
  (tabular
    (fact "about splice operations."
          (let [loc (nth elements ?n)
                loc' (e/splice loc)]
            (base/root-string loc') => ?s
            (base/sexpr loc') => ?e))
    ?n ?s                       ?e
    0  "1 [ ] [2 3] [  4  ]"    1
    1  "[1 [ ] [2 3] [  4  ]]"  1
    2  "[1 [2 3] [  4  ]]"      1
    3  "[1 [ ] 2 3 [  4  ]]"    2
    6  "[1 [ ] [2 3] 4]"        4))

(tabular
  (fact "about splicing with comment."
        (let [v (base/of-string ?data)
              loc (-> v m/down m/right e/splice)]
          (base/root-string loc) => ?s))
  ?data                       ?s
  "[1 [2\n;; comment\n3]]"    "[1 2\n;; comment\n3]"
  "[1 [;;comment\n3]]"        "[1 ;;comment\n3]"
  "[1 [;;comment\n]]"         "[1 ;;comment\n]"
  "[1 [2\n;;comment\n]]"      "[1 2\n;;comment\n]")
