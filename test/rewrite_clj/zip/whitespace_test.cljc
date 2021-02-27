(ns rewrite-clj.zip.whitespace-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as z]))

(let [space (node/spaces 1)
      linebreak (node/newlines 1)
      c0 (node/comment-node "comment")
      t0 (node/token-node 0)
      t1 (node/token-node 1)
      loc (z/edn* (node/forms-node [space linebreak t0 space t1 c0]))]
  (deftest t-predicates
    (is (z/whitespace? (-> loc z/down*)))
    (is (z/linebreak? (-> loc z/down* z/right*)))
    (is (z/whitespace-or-comment? (-> loc z/down* z/rightmost*))))
  (deftest t-skipping-whitespace-to-the-right
    (let [n (-> loc z/down* z/skip-whitespace z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-skipping-whitespace-to-the-left
    (let [n (-> loc z/down* z/rightmost* z/skip-whitespace-left z/node)]
      (is (= :token (node/tag n)))
      (is (= 1 (node/sexpr n)))))
  (deftest t-skipping-whitespace-with-a-movement-function
    (let [n (->> loc z/down* (z/skip-whitespace z/next*) z/node)]
      (is (= :token (node/tag n)))
      (is (= 0 (node/sexpr n)))))
  (deftest t-prepending=appending-spaces
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down* z/rightmost* (?left-fn 3))]
               (is (= " \n0 1   ;comment" (z/root-string n))))
             (let [n (-> loc z/down* z/rightmost* (?right-fn 3))]
               (is (= " \n0 1;comment   " (z/root-string n)))))
      z/prepend-space     z/append-space
      z/insert-space-left z/insert-space-right))
  (deftest t-prepending-appending-linebreaks
    (are [?left-fn ?right-fn]
         (do (let [n (-> loc z/down* z/rightmost* (?left-fn 3))]
               (is (= " \n0 1\n\n\n;comment" (z/root-string n))))
             (let [n (-> loc z/down* z/rightmost* (?right-fn 3))]
               (is (= " \n0 1;comment\n\n\n" (z/root-string n)))))
      z/prepend-newline     z/append-newline
      z/insert-newline-left z/insert-newline-right)))
