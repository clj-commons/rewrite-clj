(ns rewrite-clj.zip.walk-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.editz :as e]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.seqz :as sq]
            [rewrite-clj.zip.walk :as w]))

(defn- walk-order-tester [walk-fn s]
 (let [zloc (base/of-string s)
       visits (atom [])] 
   (walk-fn zloc (fn [zloc]
                     (swap! visits conj (base/string zloc))
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
           (walk-order-tester w/prewalk "(1 (2 3 (4 5) 6 (7 8)) 9)"))))
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
           (walk-order-tester w/prewalk "(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))")))))

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
           (walk-order-tester w/postwalk "(1 (2 3 (4 5) 6 (7 8)) 9)"))))
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
           (walk-order-tester w/postwalk "(1 (2 (3 (4 5) 6 (7 8)) 9 (10 (11 12) 13 (14 15)) 16))")))))

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


(defn- walker [walk-fn s]
  (let [zloc (base/of-string s)
        zloc (or (m/up zloc) zloc)]
    (-> zloc
        (walk-fn identity)
        base/root-string)))

(deftest t-zipper-wee-walks
 (are [?sample]
      (do
        (is (= ?sample (walker w/postwalk ?sample)) "postwalk")
        (is (= ?sample (walker w/prewalk ?sample)) "prewalk"))
   ""
   ";; comment"
   "1"
   "[1]"))

#?(:clj
   (deftest t-zipper-tree-larger-walks
     (are [?larger-sample]
          (let [s (slurp ?larger-sample)]
            (is (= s (walker w/postwalk s)) "postwalk")
            (is (= s (walker w/prewalk s)) "prewalk"))

       ;; 11876 lines
       "https://raw.githubusercontent.com/clojure/clojurescript/fa4b8d853be08120cb864782e4ea48826b9d757e/src/main/cljs/cljs/core.cljs"
       ;; 4745 lines
       "https://raw.githubusercontent.com/clojure/clojurescript/fa4b8d853be08120cb864782e4ea48826b9d757e/src/main/clojure/cljs/analyzer.cljc")))