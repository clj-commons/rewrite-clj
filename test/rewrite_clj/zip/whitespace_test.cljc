(ns rewrite-clj.zip.whitespace-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :refer :all]))

(let [space (node/spaces 1)
      linebreak (node/newlines 1)
      c0 (node/comment-node "comment")
      t0 (node/token-node 0)
      t1 (node/token-node 1)
      loc (base/edn* (node/forms-node [space linebreak t0 space t1 c0]))]
  (deftest t-predicates
    (is (whitespace? (-> loc z/down)))
    (is (linebreak? (-> loc z/down z/right)))
    (is (whitespace-or-comment? (-> loc z/down z/rightmost))))
  (deftest t-skipping-whitespace-to-the-right
    (let [n (-> loc z/down skip-whitespace z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-skipping-whitespace-to-the-left
    (let [n (-> loc z/down z/rightmost skip-whitespace-left z/node)]
      (is (= :token (node/tag n)))
      (is (= 1 (node/sexpr n)))))
  (deftest t-skipping-whitespace-with-a-movement-function
    (let [n (->> loc z/down (skip-whitespace z/next) z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-prepending=appending-spaces
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down z/rightmost (?left-fn 3))]
               (is (= " \n0 1   ;comment" (base/root-string n))))
             (let [n (-> loc z/down z/rightmost (?right-fn 3))]
               (is (= " \n0 1;comment   " (base/root-string n)))))
      prepend-space     append-space
      insert-space-left insert-space-right))
  (deftest t-prepending-appending-linebreaks
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down z/rightmost (prepend-newline 3))]
               (is (= " \n0 1\n\n\n;comment" (base/root-string n))))
             (let [n (-> loc z/down z/rightmost (append-newline 3))]
               (is (= " \n0 1;comment\n\n\n" (base/root-string n)))))
      prepend-newline     append-newline
      insert-newline-left insert-newline-right)))
