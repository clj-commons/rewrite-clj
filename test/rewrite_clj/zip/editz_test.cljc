(ns rewrite-clj.zip.editz-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.editz :as e]
            [rewrite-clj.zip.move :as m]))

(let [root (base/of-string "[1 \"2\" :3]")
      elements (iterate m/next root)]
  (deftest t-edit-operations
    (are [?n ?f ?s]
         (let [loc (nth elements ?n)
               loc' (e/edit loc ?f)]
           (is (= ?s (base/root-string loc'))))
      0 #(subvec % 1) "[\"2\" :3]"
      1 #(str % "_x") "[\"1_x\" \"2\" :3]"
      2 #(keyword % "k") "[1 :2/k :3]"))
  (deftest t-replace-operations
    (are [?n ?v ?s]
         (let [loc (nth elements ?n)
               loc' (e/replace loc ?v)]
           (is (= ?s (base/root-string loc'))))
      0  [1]           "[1]"
      1  #{0}          "[#{0} \"2\" :3]")))

(deftest t-edit-with-args
  (is (= "[1 102 3]" (-> "[1 2 3]"
                         base/of-string
                         m/down
                         m/right
                         (e/edit #(+ %1 %2 %3 %4) 33 23 44)
                         base/root-string))))

(deftest t-edit-uses-default-auto-resolver
  (let [root (base/of-string "[::a ::myalias/b]")]
    (is (= "[::a :b]" (-> root
                          m/down
                          m/rightmost
                          (e/edit #(if (= "??_myalias_??" (namespace %))
                                     (keyword (name %))
                                     :unexpected))
                          base/root-string)))))

(deftest t-edit-uses-custom-auto-resolver
  (let [root (base/of-string "[::a ::myalias/b]" {:auto-resolve (fn [_alias] 'my.alias.resolved)})]
    (is (= "[::a :b]" (-> root
                          m/down
                          m/rightmost
                          (e/edit #(if (= "my.alias.resolved" (namespace %))
                                     (keyword (name %))
                                     :unexpected))
                          base/root-string)))))

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

(deftest t-prefix
  (are [?in ?prefix ?expected]
       (let [zin (base/of-string ?in)
             zmod (e/prefix zin ?prefix)
             out (base/root-string zmod)]
         (is (= ?expected out)))
    "\"one\""           "123-"  "\"123-one\""
    "\"a\nbb\ncc\ndd\"" "456-"  "\"456-a\nbb\ncc\ndd\""))

(deftest t-suffix
  (are [?in ?suffix ?expected]
       (let [zin (base/of-string ?in)
             zmod (e/suffix zin ?suffix)
             out (base/root-string zmod)]
         (is (= ?expected out)))
    "\"one\""            "-123" "\"one-123\""
    "\"a\nbb\ncc\ndd\""  "-456" "\"a\nbb\ncc\ndd-456\""))