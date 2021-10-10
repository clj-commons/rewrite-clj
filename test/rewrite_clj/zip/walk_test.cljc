(ns rewrite-clj.zip.walk-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.zip :as z]))

(defn- walk-order-tester [walk-fn s]
 (let [zloc (z/of-string s)
       visits (atom [])]
   (walk-fn zloc (fn [zloc]
                     (swap! visits conj (z/string zloc))
                     zloc))
      @visits))

(deftest t-prewalk-order
  (testing "example from docstring"
    (is (= ["(1 (2 3 (4 5) 6 (7 8)) 9)"
            "1"
            "(2 3 (4 5) 6 (7 8))"
            "2"
            "3"
            "(4 5)"
            "4"
            "5"
            "6"
            "(7 8)"
            "7"
            "8"
            "9"]
           (walk-order-tester z/prewalk "(1 (2 3 (4 5) 6 (7 8)) 9)"))))
  (testing "larger example"
    (is (= ["(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))"
            "1"
            "(2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16)"
            "2"
            "(3 (4 5) 6 (7 8))"
            "3"
            "(4 5)"
            "4"
            "5"
            "6"
            "(7 8)"
            "7"
            "8"
            "9"
            "(10 (11 12) 13 (14 15))"
            "10"
            "(11 12)"
            "11"
            "12"
            "13"
            "(14 15)"
            "14"
            "15"
            "16"]
           (walk-order-tester z/prewalk "(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))")))))

(deftest t-postwalk-order
  (testing "example from docstring"
    (is (= ["1"
            "2"
            "3"
            "4"
            "5"
            "(4 5)"
            "6"
            "7"
            "8"
            "(7 8)"
            "(2 3 (4 5) 6 (7 8))"
            "9"
            "(1 (2 3 (4 5) 6 (7 8)) 9)"]
           (walk-order-tester z/postwalk "(1 (2 3 (4 5) 6 (7 8)) 9)"))))
  (testing "larger example"
    (is (= ["1"
            "2"
            "3"
            "4"
            "5"
            "(4 5)"
            "6"
            "7"
            "8"
            "(7 8)"
            "(3 (4 5) 6 (7 8))"
            "9"
            "10"
            "11"
            "12"
            "(11 12)"
            "13"
            "14"
            "15"
            "(14 15)"
            "(10 (11 12) 13 (14 15))"
            "16"
            "(2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16)"
            "(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))"]
           (walk-order-tester z/postwalk "(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))")))))

(deftest t-zipper-tree-prewalk
  (let [root (z/of-string  "[0 [1 2 3] 4]")
        not-vec? (complement z/vector?)
        inc-node #(z/edit % inc)
        inc-node-p #(if (z/vector? %) % (inc-node %))]
    (is (= "[0 [1 2 3] 4]"
           (-> root
               (z/prewalk identity)
               z/root-string)))
    (is (= "[1 [2 3 4] 5]"
           (-> root
               (z/prewalk inc-node-p)
               z/root-string)))
    (is (= "[1 [2 3 4] 5]"
           (-> root
               (z/prewalk not-vec? inc-node)
               z/root-string)))
    (is (= "[3 [4 5 6] 7]"
           (-> (iterate #(z/prewalk % not-vec? inc-node) root)
               (nth 3)
               z/root-string)))))

(deftest t-zipper-tree-postwalk
  (let [root (z/of-string "[0 [1 2 3] 4]")
        wrap-node #(z/edit % list 'x)
        wrap-node-p #(if (z/vector? %) (wrap-node %) %)]
    (is (= "[0 [1 2 3] 4]"
           (-> root
               (z/postwalk identity)
               z/root-string)))
    (is (= "([0 ([1 2 3] x) 4] x)"
           (-> root
               (z/postwalk wrap-node-p)
               z/root-string)))
    (is (= "([0 ([1 2 3] x) 4] x)"
           (-> root
               (z/postwalk z/vector? wrap-node)
               z/root-string)))))


(defn- walker [walk-fn s]
  (let [zloc (z/of-string s)
        zloc (or (z/up zloc) zloc)]
    (-> zloc
        (walk-fn identity)
        z/root-string)))

(deftest t-zipper-wee-walks
 (are [?sample]
      (do
        (is (= ?sample (walker z/postwalk ?sample)) "postwalk")
        (is (= ?sample (walker z/prewalk ?sample)) "prewalk"))
   ""
   ";; comment"
   "1"
   "[1]"))

#?(:clj
   (deftest t-zipper-tree-larger-walks
     (are [?larger-sample]
          (let [s (slurp ?larger-sample)]
            (is (= s (walker z/postwalk s)) "postwalk")
            (is (= s (walker z/prewalk s)) "prewalk"))

       ;; 11876 lines
       "https://raw.githubusercontent.com/clojure/clojurescript/fa4b8d853be08120cb864782e4ea48826b9d757e/src/main/cljs/cljs/core.cljs"
       ;; 4745 lines
       "https://raw.githubusercontent.com/clojure/clojurescript/fa4b8d853be08120cb864782e4ea48826b9d757e/src/main/clojure/cljs/analyzer.cljc")))
