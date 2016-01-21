(ns rewrite-clj.regression-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj
             [node :as node]
             [zip :as z]]
            [rewrite-clj.zip.zip :as fz]))

;; ## Regression Tests for 0.3.x -> 0.4.x

;; ### Data

(def data-string
  (str ";; This is a Project File.\n"
       "(defproject my-project \"0.1.0-SNAPSHOT\"\n"
       "  :description \"A project.\"\n"
       "  :dependencies [[a \"0.1.0\"]\n"
       "                 [b \"1.2.3\"]]\n"
       "  :repositories { \"private\" \"http://private.com/repo\" })"))

(def root (z/of-string data-string))

;; ## Tests
;;
;; These compare fast-zip's zipper operations with rewrite-clj's whitespace-aware
;; ones.

(defn- node->vec
  [n]
  (vector
    (node/tag n)
    (if (node/printable-only? n)
      (node/string n)
      (node/sexpr n))))

(defn- node->tree
  [n]
  (if (node/inner? n)
    (vec (list* (node/tag n) (map node->tree (node/children n))))
    (node->vec n)))

(def ->vec (comp node->vec z/node))
(def ->tree (comp node->tree z/node))

(fact "about top-level zipper structure and initial position"
      (node/tag (z/node root)) => :list
      (node/tag (z/root root)) => :forms)

(fact "about whitespace-aware zipper movement"
      (z/sexpr root) => list?

      (-> root fz/down fz/right ->vec) => [:whitespace " "]
      (-> root z/down z/right ->vec) => [:token 'my-project]

      (-> root fz/down fz/rightmost z/tag) => :map
      (-> root z/down z/rightmost z/tag) => :map

      (-> root fz/down fz/rightmost fz/left ->vec) => [:whitespace " "]
      (-> root z/down z/rightmost z/left ->vec) => [:token :repositories]

      (-> root fz/down fz/rightmost fz/next ->vec) => [:whitespace " "]
      (-> root z/down z/rightmost z/next ->vec) => [:token "private"]

      (-> root z/down z/leftmost ->vec) => [:token 'defproject]
      (-> root z/down z/right z/leftmost ->vec) => [:token 'defproject])

(fact "about depth-first traversal"
      (let [loc (z/of-string "(defn func [] (println 1))")
            dfs (->> (iterate z/next loc)
                     (take-while (complement z/end?)))]
        ;;dfs => (has every? truthy)
        ;;(nth dfs 6) => (nth dfs 7)
        ;;(z/end? (nth dfs 6)) => falsey
        ;;(z/end? (nth dfs 7)) => truthy
        #_(map z/sexpr dfs) #_=> #_'[(defn func [] (println 1))
                                     defn func [] (println 1)
                                     println 1 1]))

(let [tk (node/token-node :go)]
  (fact "about whitespace-aware insert/append"
        (let [loc (-> root (fz/insert-child tk) fz/down)]
          (->vec loc) => [:token :go]
          (-> loc fz/right ->vec) => [:token 'defproject])
        (let [loc (-> root (z/insert-child :go) z/down)]
          (->vec loc) => [:token :go]
          (-> loc fz/right ->vec) => [:whitespace " "]
          (-> loc z/right ->vec) => [:token 'defproject])

        (let [loc (-> root (fz/append-child tk) fz/down fz/rightmost)]
          (->vec loc) => [:token :go]
          (-> loc fz/left ->vec first) => :map)
        (let [loc (-> root (z/append-child :go) z/down z/rightmost)]
          (->vec loc) => [:token :go]
          (-> loc fz/left ->vec) => [:whitespace " "]
          (-> loc z/left ->vec first) => :map)

        (let [loc (-> root fz/down (fz/insert-right tk) fz/right)]
          (->vec loc) => [:token :go]
          (-> loc fz/left ->vec) => [:token 'defproject])
        (let [loc (-> root z/down (z/insert-right :go) z/right)]
          (->vec loc) => [:token :go]
          (-> loc fz/left ->vec) => [:whitespace " "]
          (-> loc z/left ->vec) => [:token 'defproject])

        (let [loc (-> root fz/down (fz/insert-left tk) fz/left)]
          (->vec loc) => [:token :go]
          (-> loc fz/right ->vec) => [:token 'defproject])
        (let [loc (-> root z/down (z/insert-left :go) z/left)]
          (->vec loc) => [:token :go]
          (-> loc fz/right ->vec) => [:whitespace " "]
          (-> loc z/right ->vec) => [:token 'defproject])))

(fact "about zipper modification"
      (let [root (z/of-string "[1\n 2\n 3]")]
        (->tree root)
        => [:vector
            [:token 1]
            [:newline "\n"] [:whitespace " "] [:token 2]
            [:newline "\n"] [:whitespace " "] [:token 3]]
        (node->tree (z/root root))
        => [:forms
            [:vector
             [:token 1]
             [:newline "\n"] [:whitespace " "] [:token 2]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
        (-> root z/down z/remove z/root node->tree)
        => [:forms
            [:vector
             [:token 2]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
        (-> root z/down z/right (z/replace 5) z/root node->tree)
        => [:forms
            [:vector
             [:token 1]
             [:newline "\n"] [:whitespace " "] [:token 5]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
        (-> root z/down z/right z/right (z/edit + 5) z/root node->tree)
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
      (let [root (z/of-string "[1 ;;comment\n 2]")]
        (z/sexpr root) => [1 2]
        (let [r0 (-> root z/down z/remove)]
          (z/sexpr r0) => [2]
          (z/->root-string r0) => "[;;comment\n 2]")
        (let [r0 (-> root z/down z/right z/remove)]
          (z/sexpr (z/up r0)) => [1]
          (z/->root-string r0) => "[1 ;;comment\n]"))
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

(fact "about zipper search/find traversal"
      (-> root z/down (z/find-value :description) z/right ->vec) => [:token "A project."]
      (-> root (z/find-value z/next :description) z/right ->vec) => [:token "A project."]
      (-> root (z/find-value z/next "private") z/right ->vec) => [:token "http://private.com/repo"]

      (-> root (z/find-tag z/next :map) z/down ->vec) => [:token "private"]
      (->> root z/down (iterate #(z/find-next-tag % :token)) (take-while identity) (map ->vec) (map second))
      => ['defproject 'my-project "0.1.0-SNAPSHOT" :description "A project." :dependencies :repositories]
      (->> root z/down z/rightmost (iterate #(z/find-next-tag % z/left :token)) (rest) (take-while identity) (map ->vec) (map second))
      => [:repositories :dependencies "A project." :description "0.1.0-SNAPSHOT" 'my-project 'defproject])

(fact "about zipper seq operations"
      (let [root (z/of-string "[1 2 3]")]
        root => z/seq?
        root => z/vector?
        (z/sexpr root) => [1 2 3]
        (-> root (z/get 0) ->vec) => [:token 1]
        (-> root (z/get 1) ->vec) => [:token 2]
        (-> root (z/get 2) ->vec) => [:token 3]
        (-> root (z/assoc 2 5) z/sexpr) => [1 2 5]
        (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
        (->> root (z/map #(z/edit % inc)) z/sexpr) => [2 3 4])
      (let [root (z/of-string "(1 2 3)")]
        root => z/seq?
        root => z/list?
        (z/sexpr root) => '(1 2 3)
        (-> root (z/get 0) ->vec) => [:token 1]
        (-> root (z/get 1) ->vec) => [:token 2]
        (-> root (z/get 2) ->vec) => [:token 3]
        (-> root (z/assoc 2 5) z/sexpr) => '(1 2 5)
        (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
        (->> root (z/map #(z/edit % inc)) z/sexpr) => '(2 3 4))
      (let [root (z/of-string "#{1 2 3}")]
        root => z/seq?
        root => z/set?
        (z/sexpr root) => #{1 2 3}
        (-> root (z/get 0) ->vec) => [:token 1]
        (-> root (z/get 1) ->vec) => [:token 2]
        (-> root (z/get 2) ->vec) => [:token 3]
        (-> root (z/assoc 2 5) z/sexpr) => #{1 2 5}
        (-> root (z/assoc 5 8) z/sexpr) => (throws IndexOutOfBoundsException)
        (->> root (z/map #(z/edit % inc)) z/sexpr) => #{2 3 4})
      (let [root (z/of-string "{:a 1 :b 2}")]
        root => z/seq?
        root => z/map?
        (z/sexpr root) => {:a 1 :b 2}
        (-> root (z/get :a) ->vec) => [:token 1]
        (-> root (z/get :b) ->vec) => [:token 2]
        (-> root (z/assoc :a 5) z/sexpr) => {:a 5 :b 2}
        (-> root (z/assoc :c 7) z/sexpr) => {:a 1 :b 2 :c 7}
        (->> root (z/map #(z/edit % inc)) z/sexpr) => {:a 2 :b 3}
        (->> root (z/map-keys #(z/edit % name)) z/sexpr) => {"a" 1 "b" 2}))

(fact "about quoted forms"
      (let [root (z/of-string "'a")]
        (->tree root) => [:quote [:token 'a]]
        (z/sexpr root) => '(quote a)
        (-> root z/next ->vec) => [:token 'a]
        (-> root z/next (z/replace 'b) z/up z/sexpr) => '(quote b)
        (-> root z/next (z/replace 'b) z/->root-string) => "'b"))

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

(fact "about creating zippers from files."
      (let [f (doto (java.io.File/createTempFile "rewrite.test" "")
                (.deleteOnExit))]
        (spit f data-string) => anything
        (slurp f) => data-string
        (let [loc (z/of-file f)]
          (first (->vec root)) => :list
          (node/tag (z/root root)) => :forms)))
