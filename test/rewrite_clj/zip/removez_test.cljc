(ns rewrite-clj.zip.removez-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.zip :as z]))

(deftest t-whitespace-aware-removal
  (are [?in ?n ?expected-out ?expected-out-preserve-newline]
       (let [elements (->> (z/of-string ?in)
                           (iterate z/next))
             loc      (nth elements ?n)]
         (is (= ?expected-out
                (-> loc z/remove z/root-string))
             "remove")
         (is (= ?expected-out-preserve-newline
                (-> loc z/remove-preserve-newline z/root-string))
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
  (let [root (z/of-string
              (str "  :k [[a b c]\n"
                   "      [d e f]]\n"
                   "  :keyword 0"))]
    (is (= (str "  :k [[d e f]]\n"
                "  :keyword 0")
           (-> root z/next z/down z/remove z/root-string)))))

(deftest t-removing-after-comment
  (let [loc (-> (z/of-string "; comment\nx")
                (z/rightmost)
                (z/remove))]
    (is (= "; comment\n" (z/root-string loc)))))

(deftest t-removing-at-end-of-input-preserves-an-existing-newline-at-end-of-input
  (are [?in ?expected-out ?expected-out-preserve-newline]
       (let [zloc (->> ?in
                       z/of-string
                       z/rightmost)]
         (is (= ?expected-out (->> zloc z/remove z/root-string)) "remove")
         (is (= ?expected-out-preserve-newline (->> zloc z/remove-preserve-newline z/root-string)) "remove-preserve-newline")
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
