(ns ^{ :doc "Indentation Tests" 
       :author "Yannick Scherer" }
  rewrite-clj.indent-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.indent :as i]
            [rewrite-clj.zip.utils :as u]))

;; ## Fixtures

(def data-string "{:first [1\n          2],\n :second 3}")

(def data (z/of-string data-string))

;; ## Tests

(fact "about low-level indentation (`indent`, `indent-children`)"
  (-> data (i/indent 5) z/->root-string) 
      => "     {:first [1\n               2],\n      :second 3}"
  (-> data (i/indent 5) (i/indent -5) z/->root-string) => data-string
  (-> data (i/indent 5) (i/indent -3) z/->root-string) => (-> data (i/indent 2) z/->root-string)
  (-> data z/down (i/indent 5) z/->root-string) 
      => "{     :first [1\n          2],\n :second 3}"
  (-> data (z/find-value z/next 2) (i/indent -3) z/->root-string) 
      => "{:first [1\n       2],\n :second 3}"
  (-> data (z/find-tag z/next :vector) (i/indent-children 5) z/->root-string)
      => "{:first [1\n               2],\n :second 3}" 
  (-> data (z/find-tag z/next :vector) (i/indent 5) z/->root-string)
      => "{:first      [1\n               2],\n :second 3}")

(fact "about replace/edit/insert + indent"
  (let [rloc (-> data z/down (i/replace :first-key))] 
    (z/tag rloc) => :token
    (z/->root-string rloc) => "{:first-key [1\n              2],\n :second 3}")
  (let [rloc (-> data z/down (i/replace :f))] 
    (z/tag rloc) => :token
    (z/->root-string rloc) => "{:f [1\n      2],\n :second 3}")
  (let [rloc (-> data z/down (i/edit (comp keyword #(str % "-key") name)))] 
    (z/tag rloc) => :token
    (z/->root-string rloc) => "{:first-key [1\n              2],\n :second 3}")
  (let [rloc (-> data z/down (i/edit (comp keyword #(.substring ^String % 0 1) name)))] 
    (z/tag rloc) => :token
    (z/->root-string rloc) => "{:f [1\n      2],\n :second 3}")
  (let [rloc (-> data (i/insert-left :k))]
    (z/tag rloc) => :map
    (z/->root-string rloc) => ":k {:first [1\n             2],\n    :second 3}")
  (let [rloc (-> (z/of-string "[1\n 2 [3\n    4]]") (z/find-value z/next 2) (i/insert-right "abc"))]
    (z/tag rloc) => :token
    (z/value rloc) => 2
    (z/->root-string rloc) => "[1\n 2 \"abc\" [3\n          4]]")
  (let [rloc (-> (z/of-string "[1\n 2 [3\n    4]]") (z/find-value z/next 2) i/remove)]
    (z/tag rloc) => :token
    (z/value rloc) => 1
    (z/->root-string rloc) => "[1\n [3\n  4]]"))
