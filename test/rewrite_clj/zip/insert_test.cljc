(ns rewrite-clj.zip.insert-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.test-helper :as th]))

(def zipper-opts [{} {:track-position? true}])

;; special positional markers recognized by test-helper fns
;; ⊚ - node location
;; ◬ - root :forms node

(deftest t-insert-right
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (doseq [[in                         expected]
              [["⊚[1 2 3 4]"              "⊚[1 2 3 4] x"]
               ["[⊚1 2 3 4]"              "[⊚1 x 2 3 4]"]
               ["[1 ⊚2 3 4]"              "[1 ⊚2 x 3 4]"]
               ["[1 2 ⊚3 4]"              "[1 2 ⊚3 x 4]"]
               ["[1 2 3 ⊚4]"              "[1 2 3 ⊚4 x]"]
               ["[1\n⊚ 2 3 4]"            "[1\n⊚ x 2 3 4]"]
               ["⊚\n[1 2 3 4]"            "⊚\nx [1 2 3 4]"]
               ["[1 2 3 4⊚;; comment\n]"  "[1 2 3 4⊚;; comment\nx]"]
               ["⊚;; unterminated cmt"    "⊚;; unterminated cmtx"] ;; an odd thing to do, but allowed
               ["⊚;; comment\n"           "⊚;; comment\nx"]]]
        (let [zloc (th/of-locmarked-string in zopts)]
          (is (= expected (th/root-locmarked-string (z/insert-right zloc 'x)))
              in))))))

(deftest t-insert-rigth-contrived
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (let [zloc (-> (th/of-locmarked-string "1⊚ 2" zopts) z/remove*)]
        (is (= "⊚12" (th/root-locmarked-string zloc)) "sanity pre-condition")
        (is (= "⊚1 x 2" (-> zloc
                            (z/insert-right 'x)
                            th/root-locmarked-string)))))))

(deftest t-insert-left
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (doseq [[in                        expected]
              [["⊚[1 2 3 4]"             "x ⊚[1 2 3 4]"]
               ["[⊚1 2 3 4]"             "[x ⊚1 2 3 4]"]
               ["[1 ⊚2 3 4]"             "[1 x ⊚2 3 4]"]
               ["[1 2 ⊚3 4]"             "[1 2 x ⊚3 4]"]
               ["[1 2 3 ⊚4]"             "[1 2 3 x ⊚4]"]
               ["[1\n⊚ 2 3 4]"           "[1\nx⊚ 2 3 4]"]
               ["⊚\n[1 2 3 4]"           "x⊚\n[1 2 3 4]"]
               ["⊚;; comment\n"          "x ⊚;; comment\n"]
               ["⊚;; unterminated cmt"   "x ⊚;; unterminated cmt"]]]
        (let [zloc (th/of-locmarked-string in zopts)]
          (is (= expected (th/root-locmarked-string (z/insert-left zloc 'x)))
              in))))))

(deftest t-insert-left-contrived
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (let [zloc (-> (th/of-locmarked-string "1⊚ 2" zopts) z/remove* z/right*)]
        (is (= "1⊚2" (th/root-locmarked-string zloc)) "sanity pre-condition")
        (is (= "1 x ⊚2" (-> zloc
                            (z/insert-left 'x)
                            th/root-locmarked-string)))))))

(deftest t-insert-child
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (doseq [[in                        expected]
              [["⊚[1 2 3 4]"             "⊚[x 1 2 3 4]"]
               ["⊚[]"                    "⊚[x]"]
               ["⊚[1]"                   "⊚[x 1]"]
               ["⊚[ 1]"                  "⊚[x 1]"]
               ["⊚[ 1 ]"                 "⊚[x 1 ]"]
               ["⊚[ ]"                   "⊚[x ]"]
               ["⊚[ 1 2 3 4]"            "⊚[x 1 2 3 4]"]
               ["⊚[;; comment\n1 2 3 4]" "⊚[x ;; comment\n1 2 3 4]"]
               ["◬;; unterminated cmt"   "◬x ;; unterminated cmt"]
               ["◬;; comment\n"          "◬x ;; comment\n"]]]
        (let [zloc (th/of-locmarked-string in zopts)]
          (is (= expected (th/root-locmarked-string (z/insert-child zloc 'x)))
              in))))))

(deftest t-append-child
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (doseq [[in                        expected]
              [["⊚[1 2 3 4 ]"            "⊚[1 2 3 4 x]"]
               ["⊚[]"                    "⊚[x]"]
               ["⊚[1]"                   "⊚[1 x]"]
               ["⊚[1 ]"                  "⊚[1 x]"]
               ["⊚[ 1 ]"                 "⊚[ 1 x]"]
               ["⊚[ ]"                   "⊚[ x]"]
               ["⊚[1 2 3 4;; comment\n]" "⊚[1 2 3 4;; comment\nx]"]
               ["◬;; unterminated cmt"   "◬;; unterminated cmtx"] ;; odd to do but allowed
               ["◬#! unterminated cmt"   "◬#! unterminated cmtx"] ;; try alternate comment syntax
               ["◬;; comment\n"          "◬;; comment\nx"]]]
        (let [zloc (th/of-locmarked-string in zopts)]
          (is (= expected (th/root-locmarked-string (z/append-child zloc 'x)))
              in))))))

(deftest t-different-node-types-that-allow-insertion
  (doseq [zopts zipper-opts]
    (testing (str "zipper opts " zopts)
      (doseq [[in            expected]
              [["[1 ⊚2]"     "[1 x ⊚2 y]"]
               ["(1 ⊚2)"     "(1 x ⊚2 y)"]
               ["#{1 ⊚2}"    "#{1 x ⊚2 y}"]
               ["#(1 ⊚2)"    "#(1 x ⊚2 y)"]
               ["'(1 ⊚2)"    "'(1 x ⊚2 y)"]
               ["#=(1 ⊚2)"   "#=(1 x ⊚2 y)"]
               ["#_(1 ⊚2)"   "#_(1 x ⊚2 y)"]
               ["@(f ⊚2)"    "@(f x ⊚2 y)"]]]
        (let [zloc (th/of-locmarked-string in zopts)]
          (is (= expected (-> zloc
                              (z/insert-left 'x)
                              (z/insert-right 'y)
                              (th/root-locmarked-string)))
              in))))))
