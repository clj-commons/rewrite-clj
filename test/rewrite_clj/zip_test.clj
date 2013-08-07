(ns ^{ :doc "Tests for EDN zippers."
       :author "Yannick Scherer" }
  rewrite-clj.zip-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip :as z]
            [fast-zip.core :as fz]))

;; ## Data

(def data-string
"(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.\"
  :dependencies [[a \"0.1.0\"]
                 [b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(def root (z/of-string data-string))

;; ## Tests
;;
;; These compare fast-zip's zipper operations with rewrite-clj's whitespace-aware
;; ones.

(fact "about top-level zipper structure and initial position"
  (first (z/node root)) => :list
  (first (z/root root)) => :forms)

(fact "about whitespace-aware zipper movement"
  (z/sexpr root) => list?

  (-> root fz/down fz/right fz/node) => [:whitespace " "]
  (-> root z/down z/right z/node) => [:token 'my-project]

  (-> root fz/down fz/rightmost fz/node first) => :map
  (-> root z/down z/rightmost z/node first) => :map

  (-> root fz/down fz/rightmost fz/left fz/node) => [:whitespace " "]
  (-> root z/down z/rightmost z/left z/node) => [:token :repositories]

  (-> root fz/down fz/rightmost fz/next fz/node) => [:whitespace " "]
  (-> root z/down z/rightmost z/next z/node) => [:token "private"])

(fact "about whitespace-aware insert/append"
  (let [loc (-> root (fz/insert-child [:token :go]) fz/down)]
    (fz/node loc) => [:token :go]
    (-> loc fz/right fz/node) => [:token 'defproject])
  (let [loc (-> root (z/insert-child :go) z/down)]
    (z/node loc) => [:token :go]
    (-> loc fz/right fz/node) => [:whitespace " "]
    (-> loc z/right z/node) => [:token 'defproject])
  
  (let [loc (-> root (fz/append-child [:token :go]) fz/down fz/rightmost)]
    (fz/node loc) => [:token :go]
    (-> loc fz/left fz/node first) => :map)
  (let [loc (-> root (z/append-child :go) z/down z/rightmost)]
    (z/node loc) => [:token :go]
    (-> loc fz/left fz/node) => [:whitespace " "]
    (-> loc z/left z/node first) => :map)
  
  (let [loc (-> root fz/down (fz/insert-right [:token :go]) fz/right)]
    (fz/node loc) => [:token :go]
    (-> loc fz/left fz/node) => [:token 'defproject])
  (let [loc (-> root z/down (z/insert-right :go) z/right)]
    (z/node loc) => [:token :go]
    (-> loc fz/left fz/node) => [:whitespace " "]
    (-> loc z/left z/node) => [:token 'defproject])

  (let [loc (-> root fz/down (fz/insert-left [:token :go]) fz/left)]
    (fz/node loc) => [:token :go]
    (-> loc fz/right fz/node) => [:token 'defproject])
  (let [loc (-> root z/down (z/insert-left :go) z/left)]
    (z/node loc) => [:token :go]
    (-> loc fz/right fz/node) => [:whitespace " "]
    (-> loc z/right z/node) => [:token 'defproject]))

(fact "about zipper modification"
  (let [root (z/of-string "[1\n 2\n 3]")]
    (z/node root) 
      => [:vector 
          [:token 1] 
          [:newline "\n"] [:whitespace " "] [:token 2] 
          [:newline "\n"] [:whitespace " "] [:token 3]]
    (z/root root)
      => [:forms
          [:vector 
           [:token 1] 
           [:newline "\n"] [:whitespace " "] [:token 2] 
           [:newline "\n"] [:whitespace " "] [:token 3]]]
    (-> root z/down z/remove z/root) 
      => [:forms
          [:vector 
           [:token 2] 
           [:newline "\n"] [:whitespace " "] [:token 3]]]
    (-> root z/down z/right (z/replace 5) z/root)
      => [:forms
          [:vector 
           [:token 1] 
           [:newline "\n"] [:whitespace " "] [:token 5] 
           [:newline "\n"] [:whitespace " "] [:token 3]]]
    (-> root z/down z/right z/right (z/edit + 5) z/root)
      => [:forms
          [:vector 
           [:token 1] 
           [:newline "\n"] [:whitespace " "] [:token 2] 
           [:newline "\n"] [:whitespace " "] [:token 8]]]))

(fact "about node removal (including trailing/preceding whitespace if necessary)"
  (let [root (z/of-string "[1\n2]")]
    (z/sexpr root) => [1 2]
    (let [r0 (-> root z/down z/remove)]
      (z/sexpr r0) => [2]
      (z/->root-string r0) => "[2]"))
  (let [root (z/of-string "[1 [2 3] 4]")]
    (z/sexpr root) => [1 [2 3] 4]
    (let [r0 (-> root z/down z/remove*)]
      (z/sexpr r0) => [[2 3] 4]
      (z/->root-string r0) => "[ [2 3] 4]")
    (let [r0 (-> root z/down z/remove)]
      (z/sexpr r0) => [[2 3] 4]
      (z/->root-string r0) => "[[2 3] 4]")
    (let [r0 (-> root z/down z/right z/right z/remove*)]
      (z/->root-string r0) => "[1 [2 3] ]")  
    (let [r0 (-> root z/down z/right z/right z/remove)]
      (z/sexpr r0) => 3
      (z/->root-string r0) => "[1 [2 3]]")))

(fact "about zipper splice"
  (let [r0 (z/of-string "[1 [2 3] 4]")
        r1 (-> r0 z/down z/right z/splice)]
    (z/sexpr r0) => [1 [2 3] 4]
    (z/sexpr r1) => 2
    (z/sexpr (z/up r1)) => [1 2 3 4])
  (let [r0 (z/of-string "[1 [] 4]")
        r1 (-> r0 z/down z/right z/splice)
        r2 (-> r0 z/down z/right z/splice-or-remove)]
    (z/sexpr r0) => [1 [] 4]
    r1 => falsey
    (z/sexpr r2) => 1
    (z/->root-string r2) => "[1 4]")  
  (let [r0 (z/of-string "[1 [ ] 4]")
        r1 (-> r0 z/down z/right z/splice)
        r2 (-> r0 z/down z/right z/splice-or-remove)]
    (z/sexpr r0) => [1 [] 4]
    r1 => falsey
    (z/sexpr r2) => 1
    (z/->root-string r2) => "[1 4]")
  (let [r0 (z/of-string "[1 []]")
        r1 (-> r0 z/down z/right z/splice)
        r2 (-> r0 z/down z/right z/splice-or-remove)]
    (z/sexpr r0) => [1 []]
    r1 => falsey
    (z/sexpr r2) => 1
    (z/->root-string r2) => "[1]"))

(fact "about zipper search/find traversal"
  (-> root z/down (z/find-value :description) z/right z/node) => [:token "A project."]
  (-> root (z/find-value z/next :description) z/right z/node) => [:token "A project."]
  (-> root (z/find-value z/next "private") z/right z/node) => [:token "http://private.com/repo"]

  (-> root (z/find-tag z/next :map) z/down z/node) => [:token "private"]
  (->> root z/down (iterate #(z/find-next-tag % :token)) (take-while identity) (map z/node) (map second))
    => ['defproject 'my-project "0.1.0-SNAPSHOT" :description "A project." :dependencies :repositories]
  (->> root z/down z/rightmost (iterate #(z/find-next-tag % z/left :token)) (rest) (take-while identity) (map z/node) (map second))
    => [:repositories :dependencies "A project." :description "0.1.0-SNAPSHOT" 'my-project 'defproject])

(fact "about zipper seq operations"
  (let [root (z/of-string "[1 2 3]")]
    root => z/seq?
    root => z/vector?
    (z/sexpr root) => [1 2 3]
    (-> root (z/get 0) z/node) => [:token 1]
    (-> root (z/get 1) z/node) => [:token 2]
    (-> root (z/get 2) z/node) => [:token 3]
    (-> root (z/assoc 2 5) z/sexpr) => [1 2 5]
    (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
    (->> root (z/map #(z/edit % inc)) z/sexpr) => [2 3 4])
  (let [root (z/of-string "(1 2 3)")]
    root => z/seq?
    root => z/list?
    (z/sexpr root) => '(1 2 3)
    (-> root (z/get 0) z/node) => [:token 1]
    (-> root (z/get 1) z/node) => [:token 2]
    (-> root (z/get 2) z/node) => [:token 3]
    (-> root (z/assoc 2 5) z/sexpr) => '(1 2 5)
    (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
    (->> root (z/map #(z/edit % inc)) z/sexpr) => '(2 3 4))
  (let [root (z/of-string "#{1 2 3}")]
    root => z/seq?
    root => z/set?
    (z/sexpr root) => #{1 2 3}
    (-> root (z/get 0) z/node) => [:token 1]
    (-> root (z/get 1) z/node) => [:token 2]
    (-> root (z/get 2) z/node) => [:token 3]
    (-> root (z/assoc 2 5) z/sexpr) => #{1 2 5}
    (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
    (->> root (z/map #(z/edit % inc)) z/sexpr) => #{2 3 4})
  (let [root (z/of-string "{:a 1 :b 2}")]
    root => z/seq?
    root => z/map?
    (z/sexpr root) => {:a 1 :b 2}
    (-> root (z/get :a) z/node) => [:token 1]
    (-> root (z/get :b) z/node) => [:token 2]
    (-> root (z/assoc :a 5) z/sexpr) => {:a 5 :b 2}
    (-> root (z/assoc :c 7) z/sexpr) => {:a 1 :b 2 :c 7}
    (->> root (z/map #(z/edit % inc)) z/sexpr) => {:a 2 :b 3}
    (->> root (z/map-keys #(z/edit % name)) z/sexpr) => {"a" 1 "b" 2}))

(fact "about edit scope limitation/location memoization"
  (let [root (z/of-string "[0 [1 2 3] 4]")]
    (fact "about subedit->"
      (let [r0 (-> root z/down z/right z/down z/right (z/replace 5))
            r1 (z/subedit-> root z/down z/right z/down z/right (z/replace 5))]
        (z/->root-string r0) => (z/->root-string r1)
        (z/->string r0) => "5"
        (z/->string r1) => "[0 [1 5 3] 4]"
        (z/tag r0) => :token
        (z/tag r1) => :vector))
    (fact "about subedit->>"
      (let [r0 (->> root z/down z/right (z/map #(z/edit % inc)) z/down)
            r1 (z/subedit->> root z/down z/right (z/map #(z/edit % + 1)) z/down)]
        (z/->root-string r0) => (z/->root-string r1)
        (z/->string r0) => "2"
        (z/->string r1) => "[0 [2 3 4] 4]"
        (z/tag r0) => :token
        (z/tag r1) => :vector))
    (fact "about edit->"
      (let [v (-> root z/down z/right z/down)
            r0 (-> v z/up z/right z/remove)
            r1 (z/edit-> v z/up z/right z/remove)]
        (z/->root-string r0) => (z/->root-string r1)
        (z/->string v) => "1"
        (z/->string r0) => "3"
        (z/->string r1) => "1"))
    (fact "about edit->>"
      (let [v (-> root z/down)
            r0 (->> v z/right (z/map #(z/edit % inc)) z/right)
            r1 (z/edit->> v z/right (z/map #(z/edit % inc)) z/right)]
        (z/->root-string r0) => (z/->root-string r1)
        (z/->string v) => "0"
        (z/->string r0) => "4"
        (z/->string r1) => "0"))))
