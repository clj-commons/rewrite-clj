(ns rewrite-clj.zip.seqz-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.zip :as z]))

(deftest t-predicates
  (are [?s ?p]
       (do
         (is (-> ?s z/of-string z/seq?))
         (is (-> ?s z/of-string ?p)))
    "[1 2 3]" z/vector?
    "{:a 1}" z/map?
    "#:my-prefix {:a 1}" z/namespaced-map?
    "(+ 2 3)" z/list?
    "#{1 2}" z/set?))

(deftest t-seq
  (testing "map and map-vals are equivalent and update values in maps"
    (are [?sin ?sout]
         (do
           (is (= ?sout (->> ?sin
                             z/of-string
                             (z/map-vals #(z/edit % inc))
                             z/string)))
           (is (= ?sout (->> ?sin
                             z/of-string
                             (z/map #(z/edit % inc))
                             z/string))))
      "{:a 0, :b 1}"
      "{:a 1, :b 2}"

      "#::my-ns-alias{:x 42, ::y 17}"
      "#::my-ns-alias{:x 43, ::y 18}"))

  (testing "map-keys works for maps"
    (are [?sin ?sout]
         (is (= ?sout (->> ?sin
                           z/of-string
                           (z/map-keys #(z/edit % name))
                           z/string)))
      "{:a 0, :b 1}"
      "{\"a\" 0, \"b\" 1}"))
  (testing "map works for seqs and preserves whitespace"
    (is (= "[ 5\n6\n7]" (->> "[ 1\n2\n3]"
                             z/of-string
                             (z/map #(z/edit % + 4))
                             z/string))))
  (testing "get on maps and vectors"
    (are [?sin ?key ?expected-value]
         (is (= ?expected-value (-> ?sin
                                    z/of-string
                                    (z/get ?key)
                                    z/sexpr)))
      "{:a 0 :b 1}" :a 0
      "{:a 3, :b 4}" :b 4
      "{:x 10 :y 20}" :z nil
      "[1 2 3]" 1 2))

  (testing "assoc map and vector"
    (are [?sin ?key ?value ?sout]
         (is (= ?sout (-> ?sin
                          z/of-string
                          (z/assoc ?key ?value)
                          z/string)))
      "{:a 0, :b 1}" :a 3 "{:a 3, :b 1}"
      "{:a 0, :b 1}" :c 2 "{:a 0, :b 1 :c 2}"
      "{}" :x 0 "{:x 0}"
      "[1 2 3]" 2 703 "[1 2 703]"))
  (testing "out of bounds assoc on vector should throw"
    (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error)
                 (-> "[5 10 15]" z/of-string (z/get 5))))))

(deftest t-seq-namespaced-maps
  (let [opts {:auto-resolve (fn [ns-alias]
                              (if (= :current ns-alias)
                                'my.current.ns
                                (get {'my-ns-alias 'my.aliased.ns}
                                     ns-alias
                                     (symbol (str ns-alias "-unresolved")))))}]
    (testing "get"
      (testing "default resolver"
        (let [tget (fn [s k] (-> s z/of-string (z/get k) z/string))]
          (is (= "11" (tget "#:my-prefix{:c 10 :d 11}" :my-prefix/d)))
          (is (= "42" (tget "#::my-ns-alias{:x 42, ::y 17}" :??_my-ns-alias_??/x)))
          (is (= "17" (tget "#::my-ns-alias{:x 42, ::y 17}" :?_current-ns_?/y)))))
      (testing "custom resolver"
        (let [tget (fn [s k] (-> s (z/of-string opts) (z/get k) z/string))]
          (is (= "11" (tget "#:my-prefix{:c 10 :d 11}" :my-prefix/d)))
          (is (= "42" (tget "#::my-ns-alias{:x 42, ::y 17}" :my.aliased.ns/x)))
          (is (= "17" (tget "#::my-ns-alias{:x 42, ::y 17}" :my.current.ns/y))))))
    (testing "map-keys"
      (testing "default resolver"
        (let [tmap-keys (fn [s] (->> s
                                     z/of-string
                                     (z/map-keys #(z/edit % name))
                                     z/string))]
          (is (= "#:my-prefix {\"x\" 7, \"y\" 123}" (tmap-keys "#:my-prefix {:x 7, :y 123}")))
          (is (= "#::my-ns-alias{\"x\" 42, \"y\" 17}" (tmap-keys "#::my-ns-alias{:x 42, ::y 17}")))))

      (testing "custom resolver"
        (let [tmap-keys (fn [s] (->> (z/of-string s opts)
                                     (z/map-keys #(z/edit % name))
                                     z/string))]
          (is (= "#:my-prefix {\"x\" 7, \"y\" 123}" (tmap-keys "#:my-prefix {:x 7, :y 123}")))
          (is (= "#::my-ns-alias{\"x\" 42, \"y\" 17}" (tmap-keys "#::my-ns-alias{:x 42, ::y 17}"))))))
    (testing "assoc"
      (testing "default resolver"
        (let [tassoc (fn [s k v] (-> s z/of-string (z/assoc k v) z/string))]
          (is (= "#:my-prefix{:c 10 :d \"new-d-val\"}" (tassoc "#:my-prefix{:c 10 :d 11}" :my-prefix/d "new-d-val")))
          (is (= "#::my-ns-alias{:x \"new-x-val\", ::y 17}" (tassoc "#::my-ns-alias{:x 42, ::y 17}" :??_my-ns-alias_??/x "new-x-val")))))
      (testing "custom resolver"
        (let [tassoc (fn [s k v] (-> s (z/of-string opts) (z/assoc k v) z/string))]
          (is (= "#:my-prefix{:c 10 :d \"new-d-val\"}" (tassoc "#:my-prefix{:c 10 :d 11}" :my-prefix/d "new-d-val")))
          (is (= "#::my-ns-alias{:x \"new-x-val\", ::y 17}" (tassoc "#::my-ns-alias{:x 42, ::y 17}" :my.aliased.ns/x "new-x-val"))))))))


(deftest t-sexpr-namespaced-maps
  (let [opts {:auto-resolve (fn [ns-alias]
                              (if (= :current ns-alias)
                                'my.current.ns
                                (get {'my-ns-alias 'my.aliased.ns}
                                     ns-alias
                                     (symbol (str ns-alias "-unresolved")))))}]
    (testing "unqualified map keys are unaffected by resolvers"
      (is (= {:a 1 :b 2} (-> "{:a 1 :b 2}" z/of-string z/sexpr)))
      (is (= {:a 1 :b 2} (-> "{:a 1 :b 2}" (z/of-string opts) z/sexpr))))
    (testing "qualified map keys are unaffected by resolvers"
      (is (= {:prefix/a 1 :prefix/b 2} (-> "#:prefix{:a 1 :b 2}" z/of-string z/sexpr)))
      (is (= {:prefix/a 1 :prefix/b 2} (-> "#:prefix{:a 1 :b 2}" (z/of-string opts) z/sexpr))))
    (testing "auto-resolve ns-alias map keys are affected by resolvers"
      (is (= {:??_my-ns-alias_??/a 1 :??_my-ns-alias_??/b 2}
             (-> "#::my-ns-alias{:a 1 :b 2}" z/of-string z/sexpr)))
      (is (= {:my.aliased.ns/a 1 :my.aliased.ns/b 2}
             (-> "#::my-ns-alias{:a 1 :b 2}" (z/of-string opts) z/sexpr))))
    (testing "auto-resolve current-ns map keys are affected by resolvers"
      (is (= {:?_current-ns_?/a 1 :?_current-ns_?/b 2}
             (-> "#::{:a 1 :b 2}" z/of-string z/sexpr)))
      (is (= {:my.current.ns/a 1 :my.current.ns/b 2}
             (-> "#::{:a 1 :b 2}" (z/of-string opts) z/sexpr))))
    (testing "symbols are affected by qualified maps"
      (is (= '{prefix/a 1 prefix/b 2 c 3 foo/d 4}
             (-> "#:prefix{a 1 b 2 _/c 3 foo/d 4}" z/of-string z/sexpr)))
      (is (= '{prefix/a 1 prefix/b 2 c 3 foo/d 4}
             (-> "#:prefix{a 1 b 2 _/c 3 foo/d 4}" (z/of-string opts) z/sexpr)))
      (is (= '{??_my-ns-alias_??/a 1 ??_my-ns-alias_??/b 2 c 3 foo/d 4}
             (-> "#::my-ns-alias{a 1 b 2 _/c 3 foo/d 4}" z/of-string z/sexpr)))
      (is (= '{my.aliased.ns/a 1 my.aliased.ns/b 2 c 3 foo/d 4}
             (-> "#::my-ns-alias{a 1 b 2 _/c 3 foo/d 4}" (z/of-string opts) z/sexpr)))
      (is (= '{?_current-ns_?/a 1 ?_current-ns_?/b 2 c 3 foo/d 4}
             (-> "#::{a 1 b 2 _/c 3 foo/d 4}" z/of-string z/sexpr)))
      (is (= '{my.current.ns/a 1 my.current.ns/b 2 c 3 foo/d 4}
             (-> "#::{a 1 b 2 _/c 3 foo/d 4}" (z/of-string opts) z/sexpr))))))
