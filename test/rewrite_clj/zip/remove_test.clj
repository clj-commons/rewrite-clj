(ns rewrite-clj.zip.remove-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip
             [base :as base]
             [move :as m]
             [remove :as r]]
            [fast-zip.core :as z]))

(tabular
  (fact "about whitespace-aware removal."
        (let [elements (->> (base/of-string ?data)
                            (iterate m/next))
              loc (nth elements ?n)
              loc' (r/remove loc)]
          (base/root-string loc') => ?s))
  ?data          ?n   ?s
  "[1 2 3 4]"    0    ""
  "[1 2 3 4]"    1    "[2 3 4]"
  "[1 2 3 4]"    2    "[1 3 4]"
  "[1 2 3 4]"    3    "[1 2 4]"
  "[1 2 3 4]"    4    "[1 2 3]"
  "[ 1 2 3 4]"   1    "[2 3 4]"
  "[1 2 3 4 ]"   4    "[1 2 3]"
  "[1]"          1    "[]"
  "[   1   ]"    1    "[]"
  "[;; c\n1]"    1    "[;; c\n]"
  "[1\n;; c\n2]" 1    "[;; c\n2]"
  "[1\n;; c\n2]" 2    "[1\n;; c\n]")

(fact "about more whitespace."
      (let [root (base/of-string
                   (str "  :k [[a b c]\n"
                        "      [d e f]]\n"
                        "  :keyword 0"))]
        (-> root m/next m/down r/remove base/root-string)
        => (str "  :k [[d e f]]\n"
                "  :keyword 0")))

(future-fact
  "about removing after comment."
  (let [loc (-> (base/of-string "; comment\nx")
                (z/rightmost)
                (r/remove))]
    (base/root-string loc) => "; comment"))
