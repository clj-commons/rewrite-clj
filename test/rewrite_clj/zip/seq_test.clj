(ns rewrite-clj.zip.seq-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [edit :as e]
             [seq :as sq]]))

(let [v (base/of-string "[1 2 3]")
      m (base/of-string "{:a 0, :b 1}")]
  (fact "about iteration."
        (base/string (sq/map #(e/edit % inc) v)) => "[2 3 4]")
  (fact "about iteration over map keys/values."
        (base/string (sq/map-keys #(e/edit % name) m)) => "{\"a\" 0, \"b\" 1}"
        (base/string (sq/map-vals #(e/edit % inc) m)) => "{:a 1, :b 2}")
  (fact "about get/assoc."
        (-> m (sq/get :a) base/sexpr)       => 0
        (-> m (sq/get :b) base/sexpr)       => 1
        (sq/get m :c)                       => nil?
        (-> v (sq/get 1) base/sexpr)        => 2
        (-> m (sq/assoc :a 3) base/sexpr)   => {:a 3, :b 1}
        (-> m (sq/assoc :c 2) base/sexpr)   => {:a 0, :b 1, :c 2}
        (-> v (sq/assoc 2 4)  base/sexpr)   => [1 2 4]))
