(ns rewrite-clj.zip.removez-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.removez :as r]))

(deftest t-whitespace-aware-removal
  (are [?in ?n ?expected-out ?expected-out-preserve-newline]
       (let [elements (->> (base/of-string ?in)
                           (iterate m/next))
             loc      (nth elements ?n)]
         (is (= ?expected-out
                (-> loc r/remove base/root-string))
             "remove")
         (is (= ?expected-out-preserve-newline
                (-> loc r/remove-preserve-newline base/root-string))
             "remove-preserve-newline"))
    "[1 2 3 4]"              0 ""            ""
    "[1 2 3 4]"              1 "[2 3 4]"     "[2 3 4]"
    "[1 2 3 4]"              2 "[1 3 4]"     "[1 3 4]"
    "[1 2 3 4]"              3 "[1 2 4]"     "[1 2 4]"
    "[1 2 3 4]"              4 "[1 2 3]"     "[1 2 3]"
    "[ 1 2 3 4]"             1 "[2 3 4]"     "[2 3 4]"
    "[1 2 3 4 ]"             4 "[1 2 3]"     "[1 2 3]"
    "[1]"                    1 "[]"          "[]"
    "[   1   ]"              1 "[]"          "[]"
    "[\n \n 1 \n \n]"        1 "[]"          "[\n \n\n \n]"
    "[\n \n 1 \n \n 2 \n\n]" 2 "[\n \n 1]"   "[\n \n 1 \n \n\n\n]"
    "[;; c\n1]"              1 "[;; c\n]"    "[;; c\n]"
    "[1\n;; c\n2]"           1 "[;; c\n2]"   "[\n;; c\n2]"
    "[1\n;; c\n2]"           2 "[1\n;; c\n]" "[1\n;; c\n]"
    "[1\n;; c\n2]"           1 "[;; c\n2]"   "[\n;; c\n2]"))

(deftest t-more-whitespace
  (let [root (base/of-string
              (str "  :k [[a b c]\n"
                   "      [d e f]]\n"
                   "  :keyword 0"))]
    (is (= (str "  :k [[d e f]]\n"
                "  :keyword 0")
           (-> root m/next m/down r/remove base/root-string)))))

(deftest t-removing-after-comment
  (let [loc (-> (base/of-string "; comment\nx")
                (m/rightmost)
                (r/remove))]
    (is (= "; comment\n" (base/root-string loc)))))

(deftest t-removing-at-end-of-input-preserves-an-existing-newline-at-end-of-input
  (are [?in ?expected-out ?expected-out-preserve-newline]
       (let [zloc (->> ?in
                       base/of-string
                       m/rightmost)]
         (is (= ?expected-out (->> zloc r/remove base/root-string)) "remove")
         (is (= ?expected-out-preserve-newline (->> zloc r/remove-preserve-newline base/root-string)) "remove-preserve-newline")
         true)
    "(def a 1) (del-me b 2)"
    "(def a 1)"
    "(def a 1)"

    "(def a 1) (del-me b 2)\n"
    "(def a 1)\n"
    "(def a 1)\n"

    "(def a 1)\n(del-me b 2)"
    "(def a 1)"
    "(def a 1)\n"

    "(def a 1)\n\n\n(del-me b 2)"
    "(def a 1)"
    "(def a 1)\n\n\n"

    "(def a 1) (del-me b 2)\n\n\n"
    "(def a 1)\n"
    "(def a 1)\n\n\n"

    "(def a 1)\n\n\n(del-me b 2) \n\n\n"
    "(def a 1)\n"
    "(def a 1)\n\n\n\n\n\n"

    "(def a 1)\n\n\n(def b 2)\n\n\n(del-me c 3)"
    "(def a 1)\n\n\n(def b 2)"
    "(def a 1)\n\n\n(def b 2)\n\n\n"

    "(def a 1)\n\n\n(def b 2)\n\n\n(del-me c 3)                     \n   \n"
    "(def a 1)\n\n\n(def b 2)\n"
    "(def a 1)\n\n\n(def b 2)\n\n\n\n   \n"

    "(def a 1)\n\n(del-me b 2)\n;; a comment\n"
    "(def a 1)\n;; a comment\n"
    "(def a 1)\n\n\n;; a comment\n"))
