(ns rewrite-clj.custom-zipper.utils-test
  (:require [clojure.test :refer [deftest testing is]]
            [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base])
  #?(:clj (:import clojure.lang.ExceptionInfo)))

(deftest t-remove-sibling
  (doseq [opts [{} {:track-position? true}]]
    (let [a (node/token-node 'a)
          b (node/token-node 'b)
          c (node/token-node 'c)
          d (node/token-node 'd)
          loc (zraw/down (base/of-node* (node/forms-node [a b c d]) opts))]
      (testing "remove-right"
        (let [loc' (u/remove-right loc)]
          (is (= 'a (base/sexpr loc')))
          (is (= "acd" (base/root-string loc')))))
      (testing "remove-left"
        (let [loc' (-> loc zraw/right zraw/right u/remove-left)]
          (is (= 'c (base/sexpr loc')))
          (is (= "acd" (base/root-string loc')))))
      (testing "remove-and-move-right"
        (let [loc' (u/remove-and-move-right (zraw/right loc))]
          (is (= 'c (base/sexpr loc')))
          (is (= "acd" (base/root-string loc')))))
      (testing "remove-and-move-left"
        (let [loc' (-> loc zraw/right u/remove-and-move-left)]
          (is (= 'a (base/sexpr loc')))
          (is (= "acd" (base/root-string loc'))))))))

(deftest t-remove-and-move-up
  (doseq [opts [{} {:track-position? true}]
          [s next-count expected-sexpr expected-root-string]
          [["[a [b c d]]" 4 '[c d] "[a [ c d]]"]
           ["[a [b c d]]" 6 '[b d] "[a [b  d]]"]
           ["((x) 1)"     1 '(1)   "( 1)"]
           ["((x)1)"      3 '((x)) "((x))"]]]
    (testing (str "opts " opts)
      (let [root (base/of-string s opts)
            zloc (nth (iterate zraw/next root) next-count)
            zloc' (u/remove-and-move-up zloc) ]
        (is (= expected-sexpr (base/sexpr zloc')))
        (is (= expected-root-string (base/root-string zloc')))))))

(deftest t-remove-and-move-up-throws
  ;; I can't tell you why, but shadow-cljs will sometimes, under certain versions of node and
  ;; other variables I do not understand culminate in a:
  ;;    #object[RangeError RangeError: Maximum call stack size exceeded]
  ;; when I change this test to use (thrown-with-msg? ...)
  (doseq [opts [{} {:track-position? true}]]
    (let [zloc (base/of-string "[a [b c d]]" opts)]
      (is (= "cannot remove at top" (try
                                      (u/remove-and-move-up zloc)
                                      (catch ExceptionInfo e
                                        ;; ex-message is only avail in 1.10 and we support clj >= 1.8
                                        #?(:clj (.getMessage e) :cljs (.-message e)))))
          opts))))

(deftest t-remove-and-move-left-tracks-current-position-correctly
  (doseq [[next-count expected-pos expected-root-string]
          [[3  [1 3] "[a  ccc]"]
           [5  [1 6] "[a bb ]"]
           [2  [1 2] "[abb ccc]"]]]
    (let [root (base/of-string "[a bb ccc]" {:track-position? true})
          zloc (nth (iterate zraw/next root) next-count)
          zloc (u/remove-and-move-left zloc)]
      (is (= expected-pos (zraw/position zloc)))
      (is (= expected-root-string (base/root-string zloc))))))

(deftest t-remove-and-move-right-does-not-affect-position
  (doseq [[next-count expected-pos expected-root-string]
          [[3  [1 4] "[a  ccc]"]
           [1  [1 2] "[ bb ccc]"]
           [2  [1 3] "[abb ccc]"]]]
    (let [root (base/of-string "[a bb ccc]" {:track-position? true})
          zloc (nth (iterate zraw/next root) next-count)
          zloc (u/remove-and-move-right zloc)]
      (is (= expected-pos (zraw/position zloc)))
      (is (= expected-root-string (base/root-string zloc))))))

(deftest t-remove-left-tracks-current-position-correctly
  (doseq [[next-count expected-pos expected-root-string]
          [[3  [1 3] "[abb ccc]"]
           [5  [1 6] "[a bbccc]"]]]
    (let [root (base/of-string "[a bb ccc]" {:track-position? true})
          zloc (nth (iterate zraw/next root) next-count)
          zloc (u/remove-left zloc)]
      (is (= expected-pos (zraw/position zloc)))
      (is (= expected-root-string (base/root-string zloc))))))

(deftest t-remove-and-move-up-tracks-current-position-correctly
  (doseq [[s next-count expected-pos expected-string expected-root-string]
          [["[a1 [bb4 ccc6]]" 4 [1 5] "[ ccc6]" "[a1 [ ccc6]]"]
           ["[a1 [bb4 ccc6]]" 6 [1 5] "[bb4 ]"  "[a1 [bb4 ]]"]
           ["((x) 1)"         1 [1 1] "( 1)"    "( 1)"]
           ["((x)1)"          3 [1 1] "((x))"   "((x))"]]]
    (let [root (base/of-string s {:track-position? true})
          zloc (nth (iterate zraw/next root) next-count)
          zloc (u/remove-and-move-up zloc)]
      (is (= expected-pos (zraw/position zloc)))
      (is (= expected-string (base/string zloc)))
      (is (= expected-root-string (base/root-string zloc))))))
