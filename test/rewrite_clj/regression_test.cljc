(ns rewrite-clj.regression-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.custom-zipper.core :as fz]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as z]))

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

(deftest t-top-level-zipper-structure-and-initial-position
  (is (= :list (node/tag (z/node root))))
  (is (= :forms (node/tag (z/root root)))))

(deftest t-whitespace-aware-zipper-movement
  (is (list? (z/sexpr root)))

  (is (= [:whitespace " "] (-> root fz/down fz/right ->vec)))
  (is (= [:token 'my-project] (-> root z/down z/right ->vec)))

  (is (= :map (-> root fz/down fz/rightmost z/tag)))
  (is (= :map (-> root z/down z/rightmost z/tag)))

  (is (= [:whitespace " "] (-> root fz/down fz/rightmost fz/left ->vec)))
  (is (= [:token :repositories] (-> root z/down z/rightmost z/left ->vec)))

  (is (= [:whitespace " "] (-> root fz/down fz/rightmost fz/next ->vec)))
  (is (= [:token "private"] (-> root z/down z/rightmost z/next ->vec)))

  (is (= [:token 'defproject] (-> root z/down z/leftmost ->vec)))
  (is (= [:token 'defproject] (-> root z/down z/right z/leftmost ->vec))))

(let [tk (node/token-node :go)]
  (deftest t-whitespace-aware-insertappend
    (let [loc (-> root (fz/insert-child tk) fz/down)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:token 'defproject] (-> loc fz/right ->vec))))
    (let [loc (-> root (z/insert-child :go) z/down)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:whitespace " "] (-> loc fz/right ->vec)))
      (is (= [:token 'defproject] (-> loc z/right ->vec))))

    (let [loc (-> root (fz/append-child tk) fz/down fz/rightmost)]
      (is (= [:token :go] (->vec loc)))
      (is (= :map (-> loc fz/left ->vec first))))
    (let [loc (-> root (z/append-child :go) z/down z/rightmost)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:whitespace " "] (-> loc fz/left ->vec)))
      (is (= :map (-> loc z/left ->vec first))))

    (let [loc (-> root fz/down (fz/insert-right tk) fz/right)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:token 'defproject] (-> loc fz/left ->vec))))
    (let [loc (-> root z/down (z/insert-right :go) z/right)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:whitespace " "] (-> loc fz/left ->vec)))
      (is (= [:token 'defproject] (-> loc z/left ->vec))))

    (let [loc (-> root fz/down (fz/insert-left tk) fz/left)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:token 'defproject] (-> loc fz/right ->vec))))
    (let [loc (-> root z/down (z/insert-left :go) z/left)]
      (is (= [:token :go] (->vec loc)))
      (is (= [:whitespace " "] (-> loc fz/right ->vec)))
      (is (= [:token 'defproject] (-> loc z/right ->vec))))))

(deftest t-zipper-modification
  (let [root (z/of-string "[1\n 2\n 3]")]
    (is (= [:vector
            [:token 1]
            [:newline "\n"] [:whitespace " "] [:token 2]
            [:newline "\n"] [:whitespace " "] [:token 3]]
           (->tree root)))
    (is (= [:forms
            [:vector
             [:token 1]
             [:newline "\n"] [:whitespace " "] [:token 2]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
           (node->tree (z/root root))))
    (is (= [:forms
            [:vector
             [:token 2]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
           (-> root z/down z/remove z/root node->tree)))
    (is (= [:forms
            [:vector
             [:token 1]
             [:newline "\n"] [:whitespace " "] [:token 5]
             [:newline "\n"] [:whitespace " "] [:token 3]]]
           (-> root z/down z/right (z/replace 5) z/root node->tree)))
    (is (= [:forms
            [:vector
             [:token 1]
             [:newline "\n"] [:whitespace " "] [:token 2]
             [:newline "\n"] [:whitespace " "] [:token 8]]]
           (-> root z/down z/right z/right (z/edit + 5) z/root node->tree)))))

(deftest t-node-removal-including-trailingpreceding-whitespace-if-necessary
  (let [root (z/of-string "[1\n2]")]
    (is (= [1 2] (z/sexpr root)))
    (let [r0 (-> root z/down z/remove)]
      (is (= [2] (z/sexpr r0)))
      (is (= "[2]" (z/->root-string r0)))))
  (let [root (z/of-string "[1 ;;comment\n 2]")]
    (is (= [1 2] (z/sexpr root)))
    (let [r0 (-> root z/down z/remove)]
      (is (= [2] (z/sexpr r0)))
      (is (= "[;;comment\n 2]" (z/->root-string r0))))
    (let [r0 (-> root z/down z/right z/remove)]
      (is (= [1] (z/sexpr (z/up r0))))
      (is (= "[1 ;;comment\n]" (z/->root-string r0)))))
  (let [root (z/of-string "[1 [2 3] 4]")]
    (is (= [1 [2 3] 4] (z/sexpr root)))
    (let [r0 (-> root z/down z/remove*)]
      (is (= [[2 3] 4] (z/sexpr r0)))
      (is (= "[ [2 3] 4]" (z/->root-string r0))))
    (let [r0 (-> root z/down z/remove)]
      (is (= [[2 3] 4] (z/sexpr r0)))
      (is (= "[[2 3] 4]" (z/->root-string r0))))
    (let [r0 (-> root z/down z/right z/right z/remove*)]
      (is (= "[1 [2 3] ]" (z/->root-string r0))))
    (let [r0 (-> root z/down z/right z/right z/remove)]
      (is (= 3 (z/sexpr r0)))
      (is (= "[1 [2 3]]" (z/->root-string r0))))))

(deftest t-zipper-searchfind-traversal
  (is (= [:token "A project."] (-> root z/down (z/find-value :description) z/right ->vec)))
  (is (= [:token "A project."] (-> root (z/find-value z/next :description) z/right ->vec)))
  (is (= [:token "http://private.com/repo"]
         (-> root (z/find-value z/next "private") z/right ->vec)))

  (is (= [:token "private"] (-> root (z/find-tag z/next :map) z/down ->vec)))
  (is (= ['defproject 'my-project "0.1.0-SNAPSHOT" :description "A project." :dependencies :repositories]
         (->> root z/down (iterate #(z/find-next-tag % :token)) (take-while identity) (map ->vec) (map second))))
  (is (= [:repositories :dependencies "A project." :description "0.1.0-SNAPSHOT" 'my-project 'defproject]
         (->> root z/down z/rightmost (iterate #(z/find-next-tag % z/left :token)) (rest) (take-while identity) (map ->vec) (map second)))))

(deftest t-zipper-seq-operations
  (let [root (z/of-string "[1 2 3]")]
    (is (z/seq? root))
    (is (z/vector? root))
    (is (= [1 2 3] (z/sexpr root)))
    (is (= [:token 1] (-> root (z/get 0) ->vec)))
    (is (= [:token 2] (-> root (z/get 1) ->vec)))
    (is (= [:token 3] (-> root (z/get 2) ->vec)))
    (is (= [1 2 5] (-> root (z/assoc 2 5) z/sexpr)))
    (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error) (-> root (z/assoc 5 8) z/sexpr)))
    (is (= [2 3 4] (->> root (z/map #(z/edit % inc)) z/sexpr))))
  (let [root (z/of-string "(1 2 3)")]
    (is (z/seq? root))
    (is (z/list? root))
    (is (= '(1 2 3) (z/sexpr root)))
    (is (= [:token 1] (-> root (z/get 0) ->vec)))
    (is (= [:token 2] (-> root (z/get 1) ->vec)))
    (is (= [:token 3] (-> root (z/get 2) ->vec)))
    (is (= '(1 2 5) (-> root (z/assoc 2 5) z/sexpr)))
    (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error) (-> root (z/assoc 5 8) z/sexpr)))
    (is (= '(2 3 4) (->> root (z/map #(z/edit % inc)) z/sexpr))))
  (let [root (z/of-string "#{1 2 3}")]
    (is (z/seq? root))
    (is (z/set? root))
    (is (= #{1 2 3} (z/sexpr root)))
    (is (= [:token 1] (-> root (z/get 0) ->vec)))
    (is (= [:token 2] (-> root (z/get 1) ->vec)))
    (is (= [:token 3] (-> root (z/get 2) ->vec)))
    (is (= #{1 2 5} (-> root (z/assoc 2 5) z/sexpr)))
    (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error) (-> root (z/assoc 5 8) z/sexpr)))
    (is (= #{2 3 4} (->> root (z/map #(z/edit % inc)) z/sexpr))))
  (let [root (z/of-string "{:a 1 :b 2}")]
    (is (z/seq? root))
    (is (z/map? root))
    (is (= {:a 1 :b 2} (z/sexpr root)))
    (is (= [:token 1] (-> root (z/get :a) ->vec)))
    (is (= [:token 2] (-> root (z/get :b) ->vec)))
    (is (= {:a 5 :b 2} (-> root (z/assoc :a 5) z/sexpr)))
    (is (= {:a 1 :b 2 :c 7} (-> root (z/assoc :c 7) z/sexpr)))
    (is (= {:a 2 :b 3} (->> root (z/map #(z/edit % inc)) z/sexpr)))
    (is (= {"a" 1 "b" 2} (->> root (z/map-keys #(z/edit % name)) z/sexpr)))))

(deftest t-quoted-forms
  (let [root (z/of-string "'a")]
    (is (= [:quote [:token 'a]] (->tree root)))
    (is (= '(quote a) (z/sexpr root)))
    (is (= [:token 'a] (-> root z/next ->vec)))
    (is (= '(quote b) (-> root z/next (z/replace 'b) z/up z/sexpr)))
    (is (= "'b" (-> root z/next (z/replace 'b) z/->root-string)))))

(deftest t-edit-scope-limitationlocation-memoization
  (let [root (z/of-string "[0 [1 2 3] 4]")]
    (testing "t-subedit->"
      (let [r0 (-> root z/down z/right z/down z/right (z/replace 5))
            r1 (z/subedit-> root z/down z/right z/down z/right (z/replace 5))]
        (is (= (z/->root-string r1) (z/->root-string r0)))
        (is (= "5" (z/->string r0)))
        (is (= "[0 [1 5 3] 4]" (z/->string r1)))
        (is (= :token (z/tag r0)))
        (is (= :vector (z/tag r1)))))
    (testing "t-subedit->>"
      (let [r0 (->> root z/down z/right (z/map #(z/edit % inc)) z/down)
            r1 (z/subedit->> root z/down z/right (z/map #(z/edit % + 1)) z/down)]
        (is (= (z/->root-string r1) (z/->root-string r0)))
        (is (= "2" (z/->string r0)))
        (is (= "[0 [2 3 4] 4]" (z/->string r1)))
        (is (= :token (z/tag r0)))
        (is (= :vector (z/tag r1)))))
    (testing "t-edit->"
      (let [v (-> root z/down z/right z/down)
            r0 (-> v z/up z/right z/remove)
            r1 (z/edit-> v z/up z/right z/remove)]
        (is (= (z/->root-string r1) (z/->root-string r0)))
        (is (= "1" (z/->string v)))
        (is (= "3" (z/->string r0)))
        (is (= "1" (z/->string r1)))))
    (testing "t-edit->>"
      (let [v (-> root z/down)
            r0 (->> v z/right (z/map #(z/edit % inc)) z/right)
            r1 (z/edit->> v z/right (z/map #(z/edit % inc)) z/right)]
        (is (= (z/->root-string r1) (z/->root-string r0)))
        (is (= "0" (z/->string v)))
        (is (= "4" (z/->string r0)))
        (is (= "0" (z/->string r1)))))))

#?(:clj
   (deftest t-creating-zippers-from-files
     (let [f (doto (java.io.File/createTempFile "rewrite.test" "")
               (.deleteOnExit))]
       (spit f data-string)
       (is (= data-string (slurp f)))
       (let [loc (z/of-file f)]
         (is (= :list (first (->vec loc))))
         (is (= :forms (node/tag (z/root loc))))))))
