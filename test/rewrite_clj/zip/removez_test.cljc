(ns rewrite-clj.zip.removez-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.removez :as r]))

(deftest t-whitespace-aware-removal
  (are [?data ?n ?res1 ?res2]
       (let [elements (->> (base/of-string ?data)
                           (iterate m/next))
             loc (nth elements ?n)]
         (is (= ?res1 (-> loc r/remove base/root-string)))
         (is (= ?res2 (-> loc r/remove-preserve-newline base/root-string))))
    "[1 2 3 4]"       0    ""            ""
    "[1 2 3 4]"       1    "[2 3 4]"     "[2 3 4]"
    "[1 2 3 4]"       2    "[1 3 4]"     "[1 3 4]"
    "[1 2 3 4]"       3    "[1 2 4]"     "[1 2 4]"
    "[1 2 3 4]"       4    "[1 2 3]"     "[1 2 3]"
    "[ 1 2 3 4]"      1    "[2 3 4]"     "[2 3 4]"
    "[1 2 3 4 ]"      4    "[1 2 3]"     "[1 2 3]"
    "[1]"             1    "[]"          "[]"
    "[   1   ]"       1    "[]"          "[]"
    "[\n \n 1 \n \n]" 1    "[]"          "[\n \n\n \n]"
    "[;; c\n1]"       1    "[;; c\n]"    "[;; c\n]"
    "[1\n;; c\n2]"    1    "[;; c\n2]"   "[\n;; c\n2]"
    "[1\n;; c\n2]"    2    "[1\n;; c\n]" "[1\n;; c\n]"
    "[1\n;; c\n2]"    1    "[;; c\n2]"   "[\n;; c\n2]"))

(deftest t-more-whitespace
  (let [root (base/of-string
              (str "  :k [[a b c]\n"
                   "      [d e f]]\n"
                   "  :keyword 0"))]
    (is (= (str "  :k [[d e f]]\n"
                "  :keyword 0")
           (-> root m/next m/down r/remove base/root-string)))))


(comment
  ;; future-fact
  (deftest t-removing-after-comment
    (let [loc (-> (base/of-string "; comment\nx")
                  (z/rightmost)
                  (r/remove))]
      (is (= "; comment" (base/root-string loc))))))
