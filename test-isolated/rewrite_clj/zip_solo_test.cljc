(ns rewrite-clj.zip-solo-test
  (:require [clojure.test :refer [deftest is]]
            ;; leave only rewrite-clj.zip required for these tests 
            [rewrite-clj.zip :as z]))

(deftest t-can-use-coercions-without-node-api-explicitly-required
  (is (= "[1 2 [3 4]]" (-> "[1 2]"
                           z/of-string
                           (z/append-child [3 4])
                           z/root-string))))
                             
