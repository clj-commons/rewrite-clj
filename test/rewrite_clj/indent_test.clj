(ns ^{ :doc "Indentation Tests" 
       :author "Yannick Scherer" }
  rewrite-clj.indent-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.indent :as i]))

;; ## Fixtures

(def data-string 
"{:first [1
          2],
  :second 3}")

(def data (z/of-string data-string))

;; ## Tests

(future-fact "about indentation increase"
  (-> data z/down (i/replace :first-key) z/->root-string)
    =>
"{:first-key [1
              2],
  :second 3}")

(future-fact "about indentation decrease"
  (-> data z/down (i/replace :f) z/->root-string)
    =>
"{:f [1
      2],
  :second 3}")
