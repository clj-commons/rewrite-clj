(ns rewrite-clj.zip.editz-test
  (:require [clojure.test :refer [deftest testing is]]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as z]))

(deftest t-edit-operations
  (let [root (z/of-string "[1 \"2\" :3]")
        elements (iterate z/next root)]
    (testing "edit"
      (doseq [[n f s]
              [[0 #(subvec % 1)    "[\"2\" :3]"]
               [1 #(str % "_x")    "[\"1_x\" \"2\" :3]"]
               [2 #(keyword % "k") "[1 :2/k :3]"]]]
        (let [loc (nth elements n)
              loc' (z/edit loc f)]
          (is (= s (z/root-string loc'))))))
    (testing "replace"
      (doseq [[n v s]
              [[0  [1]           "[1]"]
               [1  #{0}          "[#{0} \"2\" :3]"]]]
        (let [loc (nth elements n)
              loc' (z/replace loc v)]
          (is (= s (z/root-string loc'))))))))

(deftest t-edit-with-args
  (is (= "[1 102 3]" (-> "[1 2 3]"
                         z/of-string
                         z/down
                         z/right
                         (z/edit + 33 23 44)
                         z/root-string))))

(deftest t-edit-uses-default-auto-resolver
  (let [root (z/of-string "[::a ::myalias/b]")]
    (is (= "[::a :b]" (-> root
                          z/down
                          z/rightmost
                          (z/edit #(if (= "??_myalias_??" (namespace %))
                                     (keyword (name %))
                                     :unexpected))
                          z/root-string)))))

(deftest t-edit-uses-custom-auto-resolver
  (let [root (z/of-string "[::a ::myalias/b]" {:auto-resolve (fn [_alias] 'my.alias.resolved)})]
    (is (= "[::a :b]" (-> root
                          z/down
                          z/rightmost
                          (z/edit #(if (= "my.alias.resolved" (namespace %))
                                     (keyword (name %))
                                     :unexpected))
                          z/root-string)))))

(deftest t-splice-operations
  (let [root (z/of-string "[1 [ ] [2 3] [  4  ]]")
        elements (iterate z/next root)]
    (doseq [[n s e]
            [[0  "1 [ ] [2 3] [  4  ]"    1]
             [1  "[1 [ ] [2 3] [  4  ]]"  1]
             [2  "[1 [2 3] [  4  ]]"      1]
             [3  "[1 [ ] 2 3 [  4  ]]"    2]
             [6  "[1 [ ] [2 3] 4]"        4]]]
      (let [loc (nth elements n)
            loc' (z/splice loc)]
        (is (= s (z/root-string loc')))
        (is (= e (z/sexpr loc')))))))

(deftest t-splicing-with-comment
  (doseq [[data s]
          [["[1 [2\n;; comment\n3]]"    "[1 2\n;; comment\n3]"]
           ["[1 [;;comment\n3]]"        "[1 ;;comment\n3]"]
           ["[1 [;;comment\n]]"         "[1 ;;comment\n]"]
           ["[1 [2\n;;comment\n]]"      "[1 2\n;;comment\n]"]]]
    (let [v (z/of-string data)
          loc (-> v z/down z/right z/splice)]
      (is (= s (z/root-string loc))))))

(deftest t-replacement-using-a-hand-crafted-node
  (doseq [[node s]
          [[(node/token-node 255 "16rff")     "[16rff 2 3]"]
           [(node/integer-node 255 16)        "[0xff 2 3]"]
           [(node/integer-node 255 8)         "[0377 2 3]"]
           [(node/integer-node 9   2)         "[2r1001 2 3]"]]]
    (let [root (z/of-string "[1 2 3]")]
      (is (= s
             (-> root
                 z/next
                 (z/replace node)
                 z/root-string))))))

(deftest t-prefix
  (doseq [[in prefix expected]
          [["\"one\""           "123-"  "\"123-one\""]
           ["\"a\nbb\ncc\ndd\"" "456-"  "\"456-a\nbb\ncc\ndd\""]]]
    (let [zin (z/of-string in)
          zmod (z/prefix zin prefix)
          out (z/root-string zmod)]
      (is (= expected out)))))

(deftest t-suffix
  (doseq [[in suffix expected]
          [["\"one\""            "-123" "\"one-123\""]
           ["\"a\nbb\ncc\ndd\""  "-456" "\"a\nbb\ncc\ndd-456\""]]]
       (let [zin (z/of-string in)
             zmod (z/suffix zin suffix)
             out (z/root-string zmod)]
         (is (= expected out)))))
