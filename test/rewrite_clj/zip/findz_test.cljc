(ns rewrite-clj.zip.findz-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.zip :as z])
  #?(:clj (:import clojure.lang.ExceptionInfo)))

;; ## Fixture

(deftest t-find
  (let [s "(defn f\n  [x]\n  [(+ x 1) ::a])"
        root (z/of-string s)
        is? (fn [sexpr]
              #(and (= (z/tag %) :token)
                    (= (z/sexpr %) sexpr)))]
    (testing "find"
      (let [loc (-> root z/down* (z/find (is? 'y)))]
        (is (nil? loc)))
      (let [loc (-> root z/down* (z/find (is? 'defn)))]
        (is (= :token (z/tag loc)))
        (is (= 'defn (z/sexpr loc))))
      (let [loc (-> root z/down* (z/find (is? 'f)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (-> root z/down* z/rightmost*
                    (z/find z/left* (is? 'f)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc)))))

    (testing "find-next"
      (let [loc (-> root z/down* (z/find-next (is? 'f)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (-> root z/down* z/right* z/right*
                    (z/find-next (is? 'f)))]
        (is (nil? loc)))
      (let [tks (->> (iterate
                      (fn [node]
                        (z/find-next
                         node
                         z/next*
                         #(= (z/tag %) :token)))
                      root)
                     (take-while identity)
                     (rest))]
        (is (= 7 (count tks)))
        (is (= '[defn f x + x 1 :?_current-ns_?/a] (map z/sexpr tks)))))

    (testing "find-depth-first"
      (let [loc (-> root (z/find-depth-first (is? 'f)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (-> root (z/find-depth-first (is? 'x)))]
        (is (= :token (z/tag loc)))
        (is (= 'x (z/sexpr loc)))))

    (testing "find-next-depth-first"
      (let [loc (-> root (z/find-next-depth-first (is? 'f)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [tks (->> (iterate
                      (fn [node]
                        (z/find-next-depth-first
                         node
                         #(= (z/tag %) :token)))
                      root)
                     (take-while identity)
                     (rest))]
        (is (= 7 (count tks)))
        (is (= '[defn f x + x 1 :?_current-ns_?/a] (map z/sexpr tks)))))

    (testing "find-tag"
      (let [loc (-> root z/down* (z/find-tag :vector))]
        (is (= :vector (z/tag loc)))
        (is (= '[x] (z/sexpr loc))))
      (let [loc (-> root z/down* (z/find-tag :set))]
        (is (nil? loc))))

    (testing "find-next-tag"
      (let [loc (-> root z/down* (z/find-next-tag :vector))]
        (is (= :vector (z/tag loc)))
        (is (= '[x] (z/sexpr loc)))
        (is (= '[(+ x 1) :?_current-ns_?/a] (-> loc
                                                (z/find-next-tag :vector)
                                                z/sexpr))))
      (let [loc (-> root z/down* (z/find-next-tag :set))]
        (is (nil? loc))))

    (testing "find-token"
      (let [loc (-> root z/down*
                    (z/find-token
                     (comp #{'f 'defn} z/sexpr)))]
        (is (= :token (z/tag loc)))
        (is (= 'defn (z/sexpr loc)))))

    (testing "find-next-token"
      (let [loc (-> root z/down*
                    (z/find-next-token
                     (comp #{'f 'defn} z/sexpr)))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [locs (->> (iterate
                       (fn [node]
                         (z/find-next-token
                          node
                          z/next*
                          (comp #{'x 'defn} z/sexpr)))
                       root)
                      (take-while identity)
                      (rest))]
        (is (= '[defn x x] (map z/sexpr locs)))))

    (testing "find-value"
      (let [loc (-> root z/down* (z/find-value 'f))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (-> root z/down* (z/find-value 'y))]
        (is (nil? loc)))
      (let [loc (-> root z/down* (z/find-value #{'f 'defn}))]
        (is (= :token (z/tag loc)))
        (is (= 'defn (z/sexpr loc))))
      (let [loc (z/find-value root z/next* #{'foo 'fa :?_current-ns_?/a})]
        (is (= :token (z/tag loc)))
        (is (= :?_current-ns_?/a (z/sexpr loc))))
      (let [loc (z/find-value root z/next* :?_current-ns_?/a)]
        (is (= :token (z/tag loc)))
        (is (= :?_current-ns_?/a (z/sexpr loc)))))

    (testing "find-next-value"
      (let [loc (-> root z/down* (z/find-next-value 'f))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (-> root z/down* (z/find-next-value 'y))]
        (is (nil? loc)))
      (let [loc (-> root z/down*
                    (z/find-next-value #{'f 'defn}))]
        (is (= :token (z/tag loc)))
        (is (= 'f (z/sexpr loc))))
      (let [loc (z/find-next-value root z/next* #{'foo 'fa :?_current-ns_?/a})]
        (is (= :token (z/tag loc)))
        (is (= :?_current-ns_?/a (z/sexpr loc))))
      (let [locs (->> (iterate
                       #(z/find-next-value
                         % z/next* #{'x 'defn})
                       root)
                      (take-while identity)
                      (rest))]
        (is (= '[defn x x] (map z/sexpr locs)))))))

(deftest t-find-custom-auto-resolve
  (let [s "(defn f\n  [x]\n  [(+ x 1) ::a])"
        resolve-opts {:auto-resolve #(if (= :current %)
                                       'my.current.ns
                                       (throw (ex-info "unexpected error" {})))}
        root (z/of-string s resolve-opts)]
    (testing "find-value"
      (let [loc (z/find-value root z/next* :my.current.ns/a)]
        (is (= :token (z/tag loc)))
        (is (= :my.current.ns/a (z/sexpr loc)))))
    (testing "find-next-value"
      (let [loc (z/find-next-value root z/next* :my.current.ns/a)]
        (is (= :token (z/tag loc)))
        (is (= :my.current.ns/a (z/sexpr loc)))))))

(deftest t-find-last-by-pos
  (are [?for-position ?expected]
      ;; row        1            2      3
      ;; col        12345678901  12345  1234567890
      (let [sample "(defn hi-fn\n  [x]\n  (+ x 1))"
            actual (-> sample
                       (z/of-string {:track-position? true})
                       (z/find-last-by-pos ?for-position)
                       z/string)]
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
    ;; past and end of row
    [1 200] "\n"
    ;; past end of sample
    [3 11] nil
    [400 400] nil))

(deftest t-find-last-by-pos-invalid
  (are [?for-position]
      (let [sample (z/of-string "(def b 42)" {:track-position? true})]
        (is (thrown-with-msg? ExceptionInfo #"zipper row and col positions are ones-based"
                              (z/find-last-by-pos sample ?for-position))))
    [0 0]
    [3 0]
    [0 10]
    [-100 -200]))

(deftest find-tag-by-pos
  (is (= "[4 5 6]" (-> "[1 2 3 [4 5 6]]"
                       (z/of-string {:track-position? true})
                       (z/find-tag-by-pos {:row 1 :col 8} :vector)
                       z/string))))

(deftest find-tag-by-pos-set
  (is (= "#{4 5 6}" (-> "[1 2 3 #{4 5 6}]"
                        (z/of-string {:track-position? true})
                        (z/find-tag-by-pos {:row 1 :col 10} :set)
                        z/string))))
