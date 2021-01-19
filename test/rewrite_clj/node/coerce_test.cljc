(ns rewrite-clj.node.coerce-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.node.protocols :as node :refer [coerce]]
            [rewrite-clj.node.coerce :refer :all]
            [rewrite-clj.parser :as p]))

(deftest t-sexpr->node->sexpr-roundtrip
  (are [?sexpr]
       (let [n (coerce ?sexpr)]
         (is (satisfies? node/Node n))
         (is (string? (node/string n)))
         (is (= ?sexpr (node/sexpr n)))
         (is (= (class ?sexpr) (class (node/sexpr n)))))
;; numbers
    3
    3N
    3.14
    3.14M
    3e14
    3/4

  ;; symbol/keyword/string/...
    'symbol
    'namespace/symbol
    :keyword
    :1.5.1
    ::keyword
    ::1.5.1
    :namespace/keyword
    ""
    "hello, over there!"

  ;; seqs
    []
    [1 2 3]
    '()
    '(1 2 3)
    #{}
    #{1 2 3}

  ;; date
    #inst "2014-11-26T00:05:23"

  ;; map
    (hash-map)
    (hash-map :a 0, :b 1)))

(deftest t-sexpr->node->sexpr-roundtrip-for-regex
  (let [sexpr #"abc"
        n (coerce sexpr)]
    (is (satisfies? node/Node n))
    (is (string? (node/string n)))
    (is (= (str sexpr) (str (node/sexpr n))))
    (is (= (class sexpr) (class (node/sexpr n))))))

(deftest t-vars
  (let [n (coerce #'identity)]
    (is (satisfies? node/Node n))
    (is (= '(var clojure.core/identity) (node/sexpr n)))))

(deftest t-nil
  (let [n (coerce nil)]
    (is (satisfies? node/Node n))
    (is (= nil (node/sexpr n)))
    (is (= n (p/parse-string "nil")))))

(defrecord Foo-Bar [a])

(deftest t-records
  (let [v (Foo-Bar. 0)
        n (coerce v)]
    (is (satisfies? node/Node n))
    (is (= :reader-macro (node/tag n)))
    (is (= (pr-str v) (node/string n)))))
