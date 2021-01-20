(ns rewrite-clj.zip.whitespace-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.whitespace :as ws]))

(let [space (node/spaces 1)
      linebreak (node/newlines 1)
      c0 (node/comment-node "comment")
      t0 (node/token-node 0)
      t1 (node/token-node 1)
      loc (base/edn* (node/forms-node [space linebreak t0 space t1 c0]))]
  (deftest t-predicates
    (is (ws/whitespace? (-> loc z/down)))
    (is (ws/linebreak? (-> loc z/down z/right)))
    (is (ws/whitespace-or-comment? (-> loc z/down z/rightmost))))
  (deftest t-skipping-whitespace-to-the-right
    (let [n (-> loc z/down ws/skip-whitespace z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-skipping-whitespace-to-the-left
    (let [n (-> loc z/down z/rightmost ws/skip-whitespace-left z/node)]
      (is (= :token (node/tag n)))
      (is (= 1 (node/sexpr n)))))
  (deftest t-skipping-whitespace-with-a-movement-function
    (let [n (->> loc z/down (ws/skip-whitespace z/next) z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-prepending=appending-spaces
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down z/rightmost (?left-fn 3))]
               (is (= " \n0 1   ;comment" (base/root-string n))))
             (let [n (-> loc z/down z/rightmost (?right-fn 3))]
               (is (= " \n0 1;comment   " (base/root-string n)))))
      ws/prepend-space     ws/append-space
      ws/insert-space-left ws/insert-space-right))
  (deftest t-prepending-appending-linebreaks
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down z/rightmost (?left-fn 3))]
               (is (= " \n0 1\n\n\n;comment" (base/root-string n))))
             (let [n (-> loc z/down z/rightmost (?right-fn 3))]
               (is (= " \n0 1;comment\n\n\n" (base/root-string n)))))
      ws/prepend-newline     ws/append-newline
      ws/insert-newline-left ws/insert-newline-right)))
