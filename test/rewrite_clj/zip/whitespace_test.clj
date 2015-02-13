(ns rewrite-clj.zip.whitespace-test
  (:require [midje.sweet :refer :all]
            [clojure.zip :as z]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :refer :all]))

(let [space (node/spaces 1)
      linebreak (node/newlines 1)
      c0 (node/comment-node "comment")
      t0 (node/token-node 0)
      t1 (node/token-node 1)
      loc (z/vector-zip [space linebreak t0 space t1 c0])
      ->str #(apply str (map node/string %))]
  (fact "about predicates."
        (-> loc z/down)             => whitespace?
        (-> loc z/down z/right)     => linebreak?
        (-> loc z/down z/rightmost) => whitespace-or-comment?)
  (fact "about skipping whitespace to the right."
        (let [n (-> loc z/down skip-whitespace z/node)]
          (node/tag n) => :token
          (node/sexpr n) => 0))
  (fact "about skipping whitespace to the left."
        (let [n (-> loc z/down z/rightmost skip-whitespace-left z/node)]
          (node/tag n) => :token
          (node/sexpr n) => 1))
  (fact "about skipping whitespace with a movement function"
        (let [n (->> loc z/down (skip-whitespace z/next) z/node)]
          (node/tag n) => :token
          (node/sexpr n) => 0))
  (fact "about prepending/appending spaces."
        (let [n (-> loc z/down z/rightmost (prepend-space 3) z/root)]
          (->str n) => " \n0 1   ;comment")
        (let [n (-> loc z/down z/rightmost (append-space 3) z/root)]
          (->str n) => " \n0 1;comment   "))
  (fact "about prepending/appending linebreaks."
        (let [n (-> loc z/down z/rightmost (prepend-newline 3) z/root)]
          (->str n) => " \n0 1\n\n\n;comment")
        (let [n (-> loc z/down z/rightmost (append-newline 3) z/root)]
          (->str n) => " \n0 1;comment\n\n\n")))
