(ns rewrite-clj.zip.whitespace-test
  (:require [clojure.test :refer [deftest is testing are]]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as z]))

(deftest whitespace
  (let [space (node/spaces 1)
        linebreak (node/newlines 1)
        c0 (node/comment-node "comment")
        t0 (node/token-node 0)
        t1 (node/token-node 1)
        loc (z/of-node* (node/forms-node [space linebreak t0 space t1 c0]))]
    (testing "predicates"
      (is (z/whitespace? (-> loc z/down*)))
      (is (z/linebreak? (-> loc z/down* z/right*)))
      (is (z/whitespace-or-comment? (-> loc z/down* z/rightmost*))))
    (testing "skipping whitespace to the right"
      (let [n (-> loc z/down* z/skip-whitespace z/node)]
        (is (= :token (node/tag n)))
        (is (= 0 (node/sexpr n)))))
    (testing "skipping whitespace to the left"
      (let [n (-> loc z/down* z/rightmost* z/skip-whitespace-left z/node)]
        (is (= :token (node/tag n)))
        (is (= 1 (node/sexpr n)))))
    (testing "skipping whitespace with a movement function"
      (let [n (->> loc z/down* (z/skip-whitespace z/next*) z/node)]
        (is (= :token (node/tag n)))
        (is (= 0 (node/sexpr n)))))
    (testing "prepending appending spaces"
      (are [?left-fn ?right-fn]
           (do (let [n (-> loc z/down* z/rightmost* (?left-fn 3))]
                 (is (= " \n0 1   ;comment" (z/root-string n))))
               (let [n (-> loc z/down* z/rightmost* (?right-fn 3))]
                 (is (= " \n0 1;comment   " (z/root-string n)))))
        z/prepend-space     z/append-space
        z/insert-space-left z/insert-space-right))
    (testing "prepending appending linebreaks"
      (are [?left-fn ?right-fn]
           (do (let [n (-> loc z/down* z/rightmost* (?left-fn 3))]
                 (is (= " \n0 1\n\n\n;comment" (z/root-string n))))
               (let [n (-> loc z/down* z/rightmost* (?right-fn 3))]
                 (is (= " \n0 1;comment\n\n\n" (z/root-string n)))))
        z/prepend-newline     z/append-newline
        z/insert-newline-left z/insert-newline-right))))
