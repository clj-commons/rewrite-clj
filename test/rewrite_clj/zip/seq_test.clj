(ns rewrite-clj.zip.seq-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [edit :as e]
             [seq :as sq]]))

(let [v (base/of-string "[1 2 3]")
      m (base/of-string "{:a 0, :b 1}")
      e (base/of-string "{}")]
  (fact "about iteration."
        (base/string (sq/map #(e/edit % inc) v)) => "[2 3 4]")
  (fact "about iteration over map keys/values."
        (base/string (sq/map-keys #(e/edit % name) m)) => "{\"a\" 0, \"b\" 1}"
        (base/string (sq/map-vals #(e/edit % inc) m)) => "{:a 1, :b 2}")
  (fact "about get."
        (-> m (sq/get :a) base/sexpr)       => 0
        (-> m (sq/get :b) base/sexpr)       => 1
        (sq/get m :c)                       => nil?
        (-> v (sq/get 1) base/sexpr)        => 2)
  (fact "about assoc."
        (let [m' (sq/assoc m :a 3)]
          (base/sexpr m') => {:a 3 :b 1}
          (base/string m') => "{:a 3, :b 1}")
        (let [m' (sq/assoc m :c 2)]
          (base/sexpr m') => {:a 0 :b 1, :c 2}
          (base/string m') => "{:a 0, :b 1 :c 2}")
        (let [m' (sq/assoc e :x 0)]
          (base/sexpr m')  => {:x 0}
          (base/string m') => "{:x 0}")
        (let [v' (sq/assoc v 2 4)]
          (base/sexpr v') => [1 2 4]
          (base/string v') => "[1 2 4]")))
