(ns rewrite-clj.zip.subedit-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [subedit :refer :all]]
            [rewrite-clj.custom-zipper.core :as z]))

(let [root (base/of-string "[1 #{2 [3 4] 5} 6]")]
  (fact "about modifying subtrees."
        (let [loc (subedit-> root
                             m/next
                             m/next
                             m/next
                             (z/replace 'x))]
          (base/tag loc)    => :vector
          (base/string loc) => "[1 #{x [3 4] 5} 6]"))
  (fact "about modifying the whole tree."
        (let [loc (edit-> (-> root m/next m/next m/next)
                          m/prev m/prev
                          (z/replace 'x))]
          (base/tag loc)         => :token
          (base/string loc)      => "2"
          (base/root-string loc) => "[x #{2 [3 4] 5} 6]")))
