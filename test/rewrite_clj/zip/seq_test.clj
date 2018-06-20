(ns rewrite-clj.zip.seq-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [edit :as e]
             [seq :as sq]]))

(let [v (base/of-string "[1 2 3]")
      m (base/of-string "{:a 0, :b 1}")
      nm (base/of-string "{:a {:b 1}}")
      e (base/of-string "{}")]
  (deftest t-iteration
    (is (= "[2 3 4]" (base/string (sq/map #(e/edit % inc) v)))))
  (deftest t-iteration-over-map-keysvalues
    (is (= "{\"a\" 0, \"b\" 1}" (base/string (sq/map-keys #(e/edit % name) m))))
    (is (= "{:a 1, :b 2}" (base/string (sq/map-vals #(e/edit % inc) m)))))
  (deftest t-get
    (is (= 0 (-> m (sq/get :a) base/sexpr)))
    (is (= 1 (-> m (sq/get :b) base/sexpr)))
    (is (nil? (sq/get m :c)))
    (is (= 2 (-> v (sq/get 1) base/sexpr))))
  (deftest t-assoc
    (let [m' (sq/assoc m :a 3)]
      (is (= {:a 3 :b 1} (base/sexpr m')))
      (is (= "{:a 3, :b 1}" (base/string m'))))
    (let [m' (sq/assoc m :c 2)]
      (is (= {:a 0 :b 1, :c 2} (base/sexpr m')))
      (is (= "{:a 0, :b 1 :c 2}" (base/string m'))))
    (let [m' (sq/assoc e :x 0)]
      (is (= {:x 0} (base/sexpr m')))
      (is (= "{:x 0}" (base/string m'))))
    (let [v' (sq/assoc v 2 4)]
      (is (= [1 2 4] (base/sexpr v')))
      (is (= "[1 2 4]" (base/string v')))))
  (deftest t-assoc-in
    (let [nm' (sq/assoc-in nm [:a :b] 2)]
      (is (= {:a {:b 2}} (base/sexpr nm')))
      (is (= "{:a {:b 2}}" (base/string nm'))))
    (let [nm' (sq/assoc-in nm [:a :c] 2)]
      (is (= {:a {:b 1 :c 2}} (base/sexpr nm')))
      (is (= "{:a {:b 1 :c 2}}" (base/string nm'))))
    (let [nm' (sq/assoc-in e [:x :y :z] 0)]
      (is (= {:x {:y {:z 0}}} (base/sexpr nm')))
      (is (= "{:x {:y {:z 0}}}" (base/string nm'))))))
