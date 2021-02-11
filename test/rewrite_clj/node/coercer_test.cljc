(ns rewrite-clj.node.coercer-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.node :as node :refer [coerce]]
            [rewrite-clj.node.protocols :as protocols]
            [rewrite-clj.parser :as p]))

(deftest t-sexpr->node->sexpr-roundtrip
  (testing "simple cases roundtrip"
    (are [?sexpr expected-tag expected-type]
         (let [n (coerce ?sexpr)]
           (is (node/node? n))
           (is (= ?sexpr (node/sexpr n)))
           (is (string? (node/string n)))
           (is (= expected-tag (node/tag n)) "tag")
           (is (= expected-type (protocols/node-type n)) "node-type")
           (is (not (meta n)))
           (is (= (type ?sexpr) (type (node/sexpr n)))))

      ;; numbers

      ;; note that we do have an integer-node, but rewrite-clj never parses to it
      ;; so we never coerce to it either 
      3                      :token      :token
      3N                     :token      :token
      3.14                   :token      :token
      3.14M                  :token      :token
      3e14                   :token      :token

      ;; ratios are not valid in cljs
      #?@(:clj  [3/4         :token      :token])

      ;; symbol/keyword/string/...
      'symbol                :token      :symbol
      'namespace/symbol      :token      :symbol
      :keyword               :token      :keyword
      :1.5.1                 :token      :keyword
      ::keyword              :token      :keyword
      ::1.5.1                :token      :keyword
      :namespace/keyword     :token      :keyword

      ""                     :token      :string
      "hello, over there!"   :token      :string
      "multi\nline"          :multi-line :string
      " "                    :token      :string
      "\n"                   :multi-line :string
      "\n\n"                 :multi-line :string
      ","                    :token      :string

      ;; seqs
      []                     :vector     :seq
      [1 2 3]                :vector     :seq
      ()                     :list       :seq
      '()                    :list       :seq
      (list 1 2 3)           :list       :seq
      #{}                    :set        :seq
      #{1 2 3}               :set        :seq

      ;; date
      #inst "2014-11-26T00:05:23" :token :token))
  (testing "multi-line string newline variants are normalized"
    (let [s "hey\nyou\rover\r\nthere"
          n (node/coerce s)]
      (is (= "hey\nyou\nover\nthere" (node/sexpr n))))))

(deftest
  t-quoted-list-reader-location-metadata-elided
  (are [?sexpr expected-meta-keys]
       (let [n (coerce ?sexpr)]
         (is (node/node? n))
         (is (= expected-meta-keys (node/string n)))
         (is (string? (node/string n)))
         (is (= ?sexpr (node/sexpr n)))
         (is (not (meta n)))
         (is (= (type ?sexpr) (type (node/sexpr n)))))
    '(1 2 3) "(1 2 3)"
    '^:other-meta (4 5 6) "^{:other-meta true} (4 5 6)"))

(deftest t-maps
  (are [?sexpr]
       (let [n (coerce ?sexpr)]
         (is (node/node? n))
         (is (= :map (node/tag n)))
         (is (= :seq (protocols/node-type n)))
         (is (string? (node/string n)))
         (is (= ?sexpr (node/sexpr n)))
        ;; we do not restore to original map (hash-map or array-map),
        ;; checking if we convert to any map is sufficient
         (is (map? (node/sexpr n))))
    {}
    {:a 1 :b 2}
    (hash-map)
    (hash-map :a 0 :b 1)
    (array-map)
    (array-map :d 4 :e 5)))

(deftest t-namespaced-maps-coerce-to-maps
  (are [?sexpr]
       (let [n (coerce ?sexpr)]
         (is (node/node? n))
         (is (= :map (node/tag n)))
         (is (= :seq (protocols/node-type n)))
         (is (string? (node/string n)))
         (is (= ?sexpr (node/sexpr n)))
         (is (map? (node/sexpr n))))
    #:prefix {:a 1 :b 2}
    ;; TODO: sci barfs on auto-resolve current ns maps
    ;; #::{:c 3 :d 4}
    #::p{:e 5 :f 6}))

;; TODO: we have a regex node, do we use it?
(deftest t-sexpr->node->sexpr-roundtrip-for-regex
  (let [sexpr #"abc"
        n (coerce sexpr)]
    (is (node/node? n))
    (is (= :token (node/tag n)))
    (is (= :token (protocols/node-type n)))
    (is (string? (node/string n)))
    (is (= (str sexpr) (str (node/sexpr n))))
    (is (= (type sexpr) (type (node/sexpr n))))))

(deftest
  ^:skip-for-sci ;; sci, by design has its own var type, so skip this one for sci
  t-vars
  (let [n (coerce #'identity)]
    (is (node/node? n))
    (is (= :var (node/tag n)))
    (is (= :reader (protocols/node-type n)))
    (is (= '(var #?(:clj clojure.core/identity :cljs cljs.core/identity)) (node/sexpr n)))))

(deftest t-nil
  (let [n (coerce nil)]
    (is (node/node? n))
    (is (= :token (node/tag n)))
    (is (= :token (protocols/node-type n)))
    (is (= nil (node/sexpr n)))
    (is (= n (p/parse-string "nil")))))

(defrecord Foo-Bar [a])

(deftest
  ^:skip-for-sci ;; records have special metadata in sci, so skip this one for sci
  t-records
  (let [v (Foo-Bar. 0)
        n (coerce v)]
    (is (node/node? n))
    ;; records are represented by rewrite-clj reader macro nodes
    (is (= :reader-macro (node/tag n)))
    (is (= (pr-str v) (node/string n)))))

(deftest t-nodes-coerce-to-themselves
  (testing "parsed nodes"
    ;; lean on the parser to create node structures
    (are [?s ?tag ?type]
         (let [n (p/parse-string ?s)]
           (is (= n (node/coerce n)))
           (is (= ?tag (node/tag n)))
           (is (= ?type (protocols/node-type n))))
      ";; comment"      :comment        :comment
      "#(+ 1 %)"        :fn             :fn
      ":my-kw"          :token          :keyword
      "^:m1 [1 2 3]"    :meta           :meta
      "#:p1{:a 1 :b 2}" :namespaced-map :namespaced-map
      "'a"              :quote          :quote
      "#'var"           :var            :reader
      "#=eval"          :eval           :reader
      "@deref"          :deref          :deref
      "#mymacro 44"     :reader-macro   :reader-macro
      "#\"regex\""      :regex          :regex
      "[1 2 3]"         :vector         :seq
      "42"              :token          :token
      "sym"             :token          :symbol
      "#_ 99"           :uneval         :uneval
      " "               :whitespace     :whitespace
      ","               :comma          :comma
      "\n"              :newline        :newline))
  (testing "parsed forms nodes"
    (let [n (p/parse-string-all "(def a 1)")]
      (is (= n (node/coerce n)))
      (is (= :forms (node/tag n)))))
  (testing "map qualifier node"
    (let [n (node/map-qualifier-node false "prefix")]
      (is (= n (node/coerce n)))))
  (testing "nodes that are not parsed, but can be created manually"
    (let [n (node/integer-node 10)]
      (is (= n (node/coerce n))))
    (let [n (node/string-node "my-string")]
      (is (= n (node/coerce n))))))
