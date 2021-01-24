(ns rewrite-clj.zip.findz-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.findz :as f])
  #?(:clj (:import clojure.lang.ExceptionInfo)))

;; ## Fixture

(deftest t-find
  (let [s "(defn f\n  [x]\n  [(+ x 1) ::a])"
        root (base/of-string s)
        is? (fn [sexpr]
              #(and (= (base/tag %) :token)
                    (= (base/sexpr %) sexpr)))]
    (testing "find"
      (let [loc (-> root z/down (f/find (is? 'y)))]
        (is (nil? loc)))
      (let [loc (-> root z/down (f/find (is? 'defn)))]
        (is (= :token (base/tag loc)))
        (is (= 'defn (base/sexpr loc))))
      (let [loc (-> root z/down (f/find (is? 'f)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (-> root z/down z/rightmost
                    (f/find z/left (is? 'f)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc)))))

    (testing "find-next"
      (let [loc (-> root z/down (f/find-next (is? 'f)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (-> root z/down z/right z/right
                    (f/find-next (is? 'f)))]
        (is (nil? loc)))
      (let [tks (->> (iterate
                      (fn [node]
                        (f/find-next
                         node
                         z/next
                         #(= (base/tag %) :token)))
                      root)
                     (take-while identity)
                     (rest))]
        (is (= 7 (count tks)))
        (is (= '[defn f x + x 1 :?_current-ns_?/a] (map base/sexpr tks)))))

    (testing "find-depth-first"
      (let [loc (-> root (f/find-depth-first (is? 'f)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (-> root (f/find-depth-first (is? 'x)))]
        (is (= :token (base/tag loc)))
        (is (= 'x (base/sexpr loc)))))

    (testing "find-next-depth-first"
      (let [loc (-> root (f/find-next-depth-first (is? 'f)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [tks (->> (iterate
                      (fn [node]
                        (f/find-next-depth-first
                         node
                         #(= (base/tag %) :token)))
                      root)
                     (take-while identity)
                     (rest))]
        (is (= 7 (count tks)))
        (is (= '[defn f x + x 1 :?_current-ns_?/a] (map base/sexpr tks)))))

    (testing "find-tag"
      (let [loc (-> root z/down (f/find-tag :vector))]
        (is (= :vector (base/tag loc)))
        (is (= '[x] (base/sexpr loc))))
      (let [loc (-> root z/down (f/find-tag :set))]
        (is (nil? loc))))

    (testing "find-next-tag"
      (let [loc (-> root z/down (f/find-next-tag :vector))]
        (is (= :vector (base/tag loc)))
        (is (= '[x] (base/sexpr loc)))
        (is (= '[(+ x 1) :?_current-ns_?/a] (-> loc
                                                (f/find-next-tag :vector)
                                                base/sexpr))))
      (let [loc (-> root z/down (f/find-next-tag :set))]
        (is (nil? loc))))

    (testing "find-token"
      (let [loc (-> root z/down
                    (f/find-token
                     (comp #{'f 'defn} base/sexpr)))]
        (is (= :token (base/tag loc)))
        (is (= 'defn (base/sexpr loc)))))

    (testing "find-next-token"
      (let [loc (-> root z/down
                    (f/find-next-token
                     (comp #{'f 'defn} base/sexpr)))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [locs (->> (iterate
                       (fn [node]
                         (f/find-next-token
                          node
                          z/next
                          (comp #{'x 'defn} base/sexpr)))
                       root)
                      (take-while identity)
                      (rest))]
        (is (= '[defn x x] (map base/sexpr locs)))))

    (testing "find-value"
      (let [loc (-> root z/down (f/find-value 'f))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (-> root z/down (f/find-value 'y))]
        (is (nil? loc)))
      (let [loc (-> root z/down (f/find-value #{'f 'defn}))]
        (is (= :token (base/tag loc)))
        (is (= 'defn (base/sexpr loc))))
      (let [loc (f/find-value root z/next #{'foo 'fa :?_current-ns_?/a})]
        (is (= :token (base/tag loc)))
        (is (= :?_current-ns_?/a (base/sexpr loc))))
      (let [loc (f/find-value root z/next :?_current-ns_?/a)]
        (is (= :token (base/tag loc)))
        (is (= :?_current-ns_?/a (base/sexpr loc)))))

    (testing "find-next-value"
      (let [loc (-> root z/down (f/find-next-value 'f))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (-> root z/down (f/find-next-value 'y))]
        (is (nil? loc)))
      (let [loc (-> root z/down
                    (f/find-next-value #{'f 'defn}))]
        (is (= :token (base/tag loc)))
        (is (= 'f (base/sexpr loc))))
      (let [loc (f/find-next-value root z/next #{'foo 'fa :?_current-ns_?/a})]
        (is (= :token (base/tag loc)))
        (is (= :?_current-ns_?/a (base/sexpr loc))))
      (let [locs (->> (iterate
                       #(f/find-next-value
                         % z/next #{'x 'defn})
                       root)
                      (take-while identity)
                      (rest))]
        (is (= '[defn x x] (map base/sexpr locs)))))))

(deftest t-find-custom-auto-resolve
  (let [s "(defn f\n  [x]\n  [(+ x 1) ::a])"
        resolve-opts {:auto-resolve #(if (= :current %)
                                       'my.current.ns
                                       (throw (ex-info "unexpected error" {})))}
        root (base/of-string s resolve-opts)]
    (testing "find-value"
      (let [loc (f/find-value root z/next :my.current.ns/a)]
        (is (= :token (base/tag loc)))
        (is (= :my.current.ns/a (base/sexpr loc)))))
    (testing "find-next-value"
      (let [loc (f/find-next-value root z/next :my.current.ns/a)]
        (is (= :token (base/tag loc)))
        (is (= :my.current.ns/a (base/sexpr loc)))))))

(deftest t-find-last-by-pos
  (are [?for-position ?expected]
      ;; row        1            2      3
      ;; col        12345678901  12345  1234567890
      (let [sample "(defn hi-fn\n  [x]\n  (+ x 1))"
            actual (-> sample
                       (base/of-string {:track-position? true})
                       (f/find-last-by-pos ?for-position)
                       base/string)]
        (is (= ?expected actual)))
    [1 1] "(defn hi-fn\n  [x]\n  (+ x 1))"
    [3 10] "(defn hi-fn\n  [x]\n  (+ x 1))"
    [1 6] " "
    [1 7] "hi-fn"
    [1 10] "hi-fn"
    [1 11] "hi-fn"
    [2 4] "x"
    [2 5] "[x]"
    {:row 2 :col 5} "[x]" ;; original cljs syntax still works
    [3 8] "1"
    [3 9] "(+ x 1)"
    ;; at and end of row
    [1 12] "\n"
    ;; past and end of row TODO: was this behaviour intentional or accidental?
    [1 200] "\n"
    ;; past end of sample
    [3 11] nil
    [400 400] nil))

(deftest t-find-last-by-pos-invalid
  (are [?for-position]
      (let [sample (base/of-string "(def b 42)" {:track-position? true})]
        (is (thrown-with-msg? ExceptionInfo #"zipper row and col positions are ones-based"
                              (f/find-last-by-pos sample ?for-position))))
    [0 0]
    [3 0]
    [0 10]
    [-100 -200]))

(deftest find-tag-by-pos
  (is (= "[4 5 6]" (-> "[1 2 3 [4 5 6]]"
                       (base/of-string {:track-position? true})
                       (f/find-tag-by-pos {:row 1 :col 8} :vector)
                       base/string))))

(deftest find-tag-by-pos-set
  (is (= "#{4 5 6}" (-> "[1 2 3 #{4 5 6}]"
                        (base/of-string {:track-position? true})
                        (f/find-tag-by-pos {:row 1 :col 10} :set)
                        base/string))))
