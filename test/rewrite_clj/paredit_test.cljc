(ns rewrite-clj.paredit-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.paredit :as pe]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.test-helper :as th]))

;; special positional markers recognized by test-helper fns
;; ⊚ - node location
;; ◬ - root :forms node

(def zipper-opts [{} {:track-position? true}])

(defn- zipper-opts-desc [opts]
  (str "zipper opts " opts))

(deftest kill-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                             expected]
              [["⊚1 2 3 4"                   "◬"]
               ["  ⊚1 2 3 4"                 "⊚  "]
               ["[⊚1 2 3 4]"                 "⊚[]"]
               ["[   ⊚1 2 3 4]"              "[⊚   ]"] ;; 3 spaces are parsed as one node
               ["⊚[]"                        "◬"]
               ["[1⊚ 2 3 4]"                 "[⊚1]"]
               ["[1 ⊚2 3 4]"                 "[1⊚ ]"]
               ["[1 2 ⊚3 4]"                 "[1 2⊚ ]"]
               ["[1 2 3 ⊚4]"                 "[1 2 3⊚ ]"]
               ["[1 2]⊚ ; some comment"      "⊚[1 2]"]
               ["[⊚[1 2 3 4]]"               "⊚[]"]
               ["[1 2 3 4]⊚ 2"               "⊚[1 2 3 4]"]
               ["⊚[1 2 3 4] 5"               "◬"]
               ["[1 [2 3]⊚ 4 5]"             "[1 ⊚[2 3]]"]
               ["[1 [2 [3 [4]]]⊚ 5 6]"       "[1 ⊚[2 [3 [4]]]]"]
               ["[1\n[2⊚\n[3\n4]\n5]]"       "[1\n[⊚2]]"]
               ["[1\n[2\n[3 \n⊚  4]\n5]]"    "[1\n[2\n[3 ⊚\n]\n5]]"]
               ["[ \n  \n  \n ⊚1 2 3 4]"     "[ \n  \n  \n⊚ ]"]
               ["[ ⊚\n  \n 1 2 3 4]"         "[⊚ ]"]
               ["[ \n  ⊚\n 1 2 3 4]"         "[ \n⊚  ]"] ;; multiple spaces are a single node
               ["[ \n⊚  \n 1 2 3 4]"         "[ ⊚\n]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) before changes")
            (is (= expected (-> zloc pe/kill th/root-locmarked-string)))))))))

(deftest kill-at-pos-test
  ;; for this pos fn test, ⊚ in `s` represents character row/col for the `pos`
  ;; ⊚ in `expected` is at zipper node granularity
  (doseq [[s                                expected]
          [["[⊚] 5"                         "◬5"] ;; TODO: questionable, our pos is now at :forms root node
           ["; dill⊚dall"                   "⊚; dill"]
           ["(str \"He⊚llo \" \"World!\")"  "(str ⊚\"He\" \"World!\")"]
           [(str "(str \""
                 "First line\n"
                 "  Second⊚ Line\n"
                 "    Third Line\n"
                 "        \")")             (str "(str ⊚\""
                                                 "First line\n"
                                                 "  Second\")")]
           [(str "\n"
                 "(println \"Hello⊚\n"
                 "         There"
                 "         World\")")
            "\n(println ⊚\"Hello\")"

            ["⊚\"\""                         "◬"]]]]
    (let [{:keys [pos s]} (th/pos-and-s s)
          zloc (z/of-string* s {:track-position? true})]
      (doseq [pos [pos [(:row pos) (:col pos)]]]
        (testing (str s " @pos " pos)
          (is (= expected (-> zloc (pe/kill-at-pos pos) th/root-locmarked-string))))))))

(deftest kill-one-at-pos-test
  ;; for this pos fn test, ⊚ in `s` represents character row/col the the `pos`
  ;; ⊚ in `expected` is at zipper node granularity
  (doseq [[s                              expected]
          [["(+ ⊚100 200)"                "(⊚+ 200)"]
           ["(foo ⊚(bar do))"             "(⊚foo)"]
           ["[10⊚ 20 30]"                 "[⊚10 30]"]      ;; searches forward for node
           ["[10 ⊚20 30]"                 "[⊚10 30]"]
           ["[[10]⊚ 20 30]"               "[⊚[10] 30]"]    ;; searches forward for node
           ["[[10] ⊚20 30]"               "[⊚[10] 30]"]    ;; navigates left after delete when possible
           ["[10] [⊚20 30]"               "[10] ⊚[30]"]
           ["[⊚10\n 20\n 30]"             "⊚[20\n 30]"]
           ["[10\n⊚ 20\n 30]"             "[⊚10\n 30]"]
           ["[10\n 20\n⊚ 30]"             "[10\n ⊚20]"]
           ["[⊚10 20 30]"                 "⊚[20 30]"]
           ["⊚[10 20 30]"                 "◬"]

           ;; in comment
           ["; hello⊚ world"              "⊚; hello world"]   ;; only kill word if word spans pos
           ["; hello ⊚world"              "⊚; hello "]        ;; at w of world, kill it
           ["; ⊚hello world"              "⊚;  world"]        ;; at h of hello, kill it
           ["; hello worl⊚d"              "⊚; hello "]        ;; at d of world, kill it
           [";⊚ hello world"              "⊚; hello world"]   ;; not in any word, no-op          ;;

           ;; in string
           ["\"hello⊚ world\""            "⊚\"hello world\""] ;; not in word, no-op
           ["\"hello ⊚world\""            "⊚\"hello \""]
           ["\"hello worl⊚d\""            "⊚\"hello \""]
           ["\"⊚hello world\""            "⊚\" world\""]
           ["\"⊚foo bar do\n lorem\""     "⊚\" bar do\n lorem\""]
           ["\"foo bar do\n⊚ lorem\""     "⊚\"foo bar do\n lorem\""] ;; not in word, no-op
           ["\"foo bar do\n ⊚lorem\""     "⊚\"foo bar do\n \""]
           ["\"foo bar ⊚do\n lorem\""     "⊚\"foo bar \n lorem\""]]]
    (let [{:keys [pos s]} (th/pos-and-s s)
          zloc (z/of-string* s {:track-position? true})]
      (doseq [pos [pos [(:row pos) (:col pos)]]]
        (testing (str s " @pos " pos)
          (is (= expected (-> zloc (pe/kill-one-at-pos pos) th/root-locmarked-string))))))))

(deftest slurp-forward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s expected]
              [["[[1 ⊚2] 3 4]"                        "[[1 ⊚2 3] 4]"]
               ["[[⊚1 2] 3 4]"                        "[[⊚1 2 3] 4]"]
               ["[[1⊚ 2] 3 4]"                        "[[1⊚ 2 3] 4]"]
               ["[⊚[] 1 2 3]"                         "[[⊚1] 2 3]"]
               ["[⊚[1] 2 3]"                          "[⊚[1] 2 3]"]
               ["[⊚[1] 2 3] 4"                        "[⊚[1] 2 3 4]"]
               ["[[1⊚ 2] 3 4]"                        "[[1⊚ 2 3] 4]"]
               ["[[[⊚1 2]] 3 4]"                      "[[[⊚1 2] 3] 4]"]
               ["[[[[[⊚1 2]]]] 3 4]"                  "[[[[[⊚1 2]]] 3] 4]"]
               ["[1 [⊚2 [3 4]] 5]"                    "[1 [⊚2 [3 4] 5]]"]
               ["[[1 [⊚2]] 3 4]"                      "[[1 [⊚2] 3] 4]"]
               ["(get ⊚:x) :a"                        "(get ⊚:x :a)"]
               ["(get ⊚{}) :a"                        "(get ⊚{} :a)"]
               ["(get ⊚{} :a)"                        "(get {⊚:a})"]
               ["(get ⊚{:a} :b)"                      "(get ⊚{:a} :b)"]
               ["⊚[#_uneval] :a"                      "⊚[#_uneval] :a"]
               ["[⊚#_uneval] :a"                      "[⊚#_uneval :a]"]
               ["(a ⊚[    ] b) c"                     "(a [    ⊚b]) c"]
               ["(a [  ⊚b   ] c) d"                   "(a [  ⊚b   c]) d"]
               ["(let [⊚dill]\n  {:a 1}\n  {:b 2})"   "(let [⊚dill\n{:a 1}]\n  {:b 2})"]
               ["(a [⊚foo]\n#_b  c)"                  "(a [⊚foo\n#_b]  c)"]
               ["(a ⊚b c);; comment\n(d e f)"         "(a ⊚b c ;; comment\n(d e f))"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) before changes")
          (is (= expected (-> zloc pe/slurp-forward th/root-locmarked-string))))))))

(deftest slurp-foward-into-test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                                      expected-from-parent               expected-from-current]
              [["[[1 ⊚2] 3 4]"                        "[[1 ⊚2 3] 4]"                     :ditto]
               ["[[⊚1 2] 3 4]"                        "[[⊚1 2 3] 4]"                     :ditto]
               ["[[1⊚ 2] 3 4]"                        "[[1⊚ 2 3] 4]"                     :ditto]
               ["[⊚[] 1 2 3]"                         "[⊚[] 1 2 3]"                      "[⊚[1] 2 3]"]
               ["[⊚[1] 2 3]"                          "[⊚[1] 2 3]"                       "[⊚[1 2] 3]"]
               ["[[⊚1] 2 3]"                          "[[⊚1 2] 3]"                       :ditto]
               ["[[1⊚ 2] 3 4]"                        "[[1⊚ 2 3] 4]"                     :ditto]
               ["[[[⊚1 2]] 3 4]"                      "[[[⊚1 2] 3] 4]"                   :ditto]
               ["[[[[[⊚1 2]]]] 3 4]"                  "[[[[[⊚1 2]]] 3] 4]"               :ditto]
               ["[1 [⊚2 [3 4]] 5]"                    "[1 [⊚2 [3 4] 5]]"                 :ditto]
               ["[[1 [⊚2]] 3 4]"                      "[[1 [⊚2] 3] 4]"                   :ditto]
               ["[:a :b [:c :d :e [⊚:f]]] :g"         "[:a :b [:c :d :e [⊚:f]] :g]"      :ditto]
               ["(get ⊚:x) :a"                        "(get ⊚:x :a)"                     :ditto]
               ["(get ⊚{}) :a"                        "(get ⊚{} :a)"                     :ditto]
               ["(get ⊚{} :a)"                        "(get ⊚{} :a)"                     "(get ⊚{:a})"]
               ["(get ⊚{:a} :b)"                      "(get ⊚{:a} :b)"                   "(get ⊚{:a :b})"]
               ["⊚[#_uneval] :a"                      "⊚[#_uneval] :a"                   "⊚[#_uneval :a]"]
               ["[⊚#_uneval] :a"                      "[⊚#_uneval :a]"                   "[⊚#_uneval :a]"]
               ["(a ⊚[    ] b) c"                     "(a ⊚[    ] b c)"                  "(a ⊚[    b]) c"]
               ["(a [  ⊚b   ] c) d"                   "(a [  ⊚b   c]) d"                 :ditto]
               ["(let [⊚dill]\n  {:a 1}\n  {:b 2})"   "(let [⊚dill\n{:a 1}]\n  {:b 2})"  :ditto]
               ["(a [⊚foo]\n#_b  c)"                  "(a [⊚foo\n#_b]  c)"               :ditto
               ["(a ⊚b c);; comment\n(d e f)"         "(a ⊚b c ;; comment\n(d e f))"     :ditto]]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res-from-parent (pe/slurp-forward-into zloc {:from :parent})
                res-default (pe/slurp-forward-into zloc)
                res-from-current (pe/slurp-forward-into zloc {:from :current})]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected-from-parent (th/root-locmarked-string res-default)) "root-string after default slurp")
            (is (= expected-from-parent (th/root-locmarked-string res-from-parent)) "root-string after from parent")
            (if (= :ditto expected-from-current)
              (is (= expected-from-parent (th/root-locmarked-string res-from-current)) "root-string after from current same as from parent")
              (is (= expected-from-current (th/root-locmarked-string res-from-current)) "root-string after from current"))))))))

(deftest slurp-foward-fully-into-test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                                      expected-from-parent          expected-from-current ]
              [["[1 [⊚2] 3 4]"                        "[1 [⊚2 3 4]]"                :ditto]
               ["[1 ⊚[] 2 3 4] 5"                     "[1 ⊚[] 2 3 4 5]"             "[1 ⊚[2 3 4]] 5"]
               ["[[[1 ⊚[] 2 3 4] 5 6] 7] 8"           "[[[1 ⊚[] 2 3 4 5 6]] 7] 8"   "[[[1 ⊚[2 3 4]] 5 6] 7] 8" ]
               ["[[1 ⊚[]] 3 4]"                       "[[1 ⊚[] 3 4]]"               "[[1 ⊚[3 4]]]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res-from-parent (pe/slurp-forward-fully-into zloc {:from :parent})
                res-default (pe/slurp-forward-fully-into zloc)
                res-from-current (pe/slurp-forward-fully-into zloc {:from :current})]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected-from-parent (th/root-locmarked-string res-default)) "root-string after default slurp")
            (is (= expected-from-parent (th/root-locmarked-string res-from-parent)) "root-string after from parent")
            (if (= :ditto expected-from-current)
              (is (= expected-from-parent (th/root-locmarked-string res-from-current)) "root-string after from current same as from parent")
              (is (= expected-from-current (th/root-locmarked-string res-from-current)) "root-string after from current"))))))))

(deftest slurp-foward-fully-test ;; deprecated but we still need to test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                                      expected]
              [["[1 [⊚2] 3 4]"                        "[1 [⊚2 3 4]]"]
               ["[1 ⊚[] 2 3 4]"                       "[1 [⊚2 3 4]]"]
               ["[[[1 ⊚[] 2 3 4] 5] 6] 7"             "[[[1 [⊚2 3 4]] 5] 6] 7"]
               ["[[1 ⊚[]] 3 4]"                       "[[1 [⊚3 4]]]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res (pe/slurp-forward-fully zloc)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected (th/root-locmarked-string res)) "root-string after")))))))

(deftest slurp-backward-into-test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                       expected-from-parent         expected-from-current]
              [["[1 2 [⊚3 4]]"         "[1 [2 ⊚3 4]]"               :ditto]
               ["[1 2 [3 ⊚4]]"         "[1 [2 3 ⊚4]]"               :ditto]
               ["[1 2 3 4 ⊚[]]"        "[1 2 3 4 ⊚[]]"              "[1 2 3 ⊚[4]]"]
               ["[1 2 [[3 ⊚4]]]"       "[1 [2 [3 ⊚4]]]"             :ditto]
               ["[1 2 [[[3 ⊚4]]]]"     "[1 [2 [[3 ⊚4]]]]"           :ditto]
               [":a [⊚[] :b]"          "[:a ⊚[] :b]"                :ditto]
               ["[:a ⊚[] :b]"          "[:a ⊚[] :b]"                "[⊚[:a] :b]"]
               ["[⊚:a [] :b]"          "[⊚:a [] :b]"                :ditto]
               ["[1 2 \n \n [⊚3 4]]"   "[1 [2\n\n⊚3 4]]"            :ditto]
               ["[1 2 ;dill\n [⊚3 4]]" "[1 [2 ;dill\n⊚3 4]]"        :ditto]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res-from-parent (pe/slurp-backward-into zloc {:from :parent})
                res-default (pe/slurp-backward-into zloc)
                res-from-current (pe/slurp-backward-into zloc {:from :current})]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected-from-parent (th/root-locmarked-string res-default)) "root-string after default slurp")
            (is (= expected-from-parent (th/root-locmarked-string res-from-parent)) "root-string after from parent")
            (if (= :ditto expected-from-current)
              (is (= expected-from-parent (th/root-locmarked-string res-from-current)) "root-string after from current same as from parent")
              (is (= expected-from-current (th/root-locmarked-string res-from-current)) "root-string after from current"))))))))

(deftest slurp-backward-test ;; deprecated but we still need to test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                       expected]
              [["[1 2 [⊚3 4]]"         "[1 [2 ⊚3 4]]"]
               ["[1 2 [3 ⊚4]]"         "[1 [2 3 ⊚4]]"]
               ["[1 2 3 4 ⊚[]]"        "[1 2 3 [⊚4]]"]
               ["[1 2 [[3 ⊚4]]]"       "[1 [2 [3 ⊚4]]]"]
               ["[1 2 [[[3 ⊚4]]]]"     "[1 [2 [[3 ⊚4]]]]"]
               [":a [⊚[] :b]"          "[:a ⊚[] :b]"]
               ["[:a ⊚[] :b]"          "[[⊚:a] :b]"]
               ["[⊚:a [] :b]"          "[⊚:a [] :b]"]
               ["[1 2 \n \n [⊚3 4]]"   "[1 [2\n\n⊚3 4]]"]
               ["[1 2 ;dill\n [⊚3 4]]" "[1 [2 ;dill\n⊚3 4]]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res (pe/slurp-backward zloc)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected (th/root-locmarked-string res)) "root-string after")))))))

(deftest slurp-backward-fully-into-test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                          expected-from-parent    expected-from-current]
              [["[1 2 3 [⊚4] 5]"          "[[1 2 3 ⊚4] 5]"        :ditto]
               ["[1 2 3 4 ⊚[] 5]"         "[1 2 3 4 ⊚[] 5]"       "[⊚[1 2 3 4] 5]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res-from-parent (pe/slurp-backward-fully-into zloc {:from :parent})
                res-default (pe/slurp-backward-fully-into zloc)
                res-from-current (pe/slurp-backward-fully-into zloc {:from :current})]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected-from-parent (th/root-locmarked-string res-default)) "root-string after default slurp")
            (is (= expected-from-parent (th/root-locmarked-string res-from-parent)) "root-string after from parent")
            (if (= :ditto expected-from-current)
              (is (= expected-from-parent (th/root-locmarked-string res-from-current)) "root-string after from current same as from parent")
              (is (= expected-from-current (th/root-locmarked-string res-from-current)) "root-string after from current"))))))))

(deftest slurp-backward-fully-test ;; deprecated but we still need to test
  (doseq [opts zipper-opts]
    (testing (str "zipper opts " opts)
      (doseq [[s                          expected]
              [["[1 2 3 [⊚4] 5]"          "[[1 2 3 ⊚4] 5]"]
               ["[1 2 3 4 ⊚[] 5]"         "[[⊚1 2 3 4] 5]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)
                res (pe/slurp-backward-fully zloc)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) s before change")
            (is (= expected (th/root-locmarked-string res)) "root-string after")))))))

(deftest barf-forward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                                                   expected]
              [["[[1 ⊚2 3] 4]"                                     "[[1 ⊚2] 3 4]"]
               ["[[⊚1 2 3] 4]"                                     "[[⊚1 2] 3 4]" ]
               ["[[1 2 ⊚3] 4]"                                     "[[1 2] ⊚3 4]"]
               ["[[1 2 3⊚ ] 4]"                                    "[[1 2] ⊚3 4]"]
               ["[[1 2⊚ 3] 4]"                                     "[[1 2] ⊚3 4]"]
               ["[[⊚1] 2]"                                         "[[] ⊚1 2]"]
               ["(⊚(x) 1)"                                         "(⊚(x)) 1"]
               ["(⊚(x)1)"                                          "(⊚(x)) 1"]
               ["(⊚(x)(y))"                                        "(⊚(x)) (y)"]
               ["[⊚{:a 1} {:b 2} {:c 3}]"                          "[⊚{:a 1} {:b 2}] {:c 3}"]
               ["[{:a 1} ⊚{:b 2} {:c 3}]"                          "[{:a 1} ⊚{:b 2}] {:c 3}"]
               ["[{:a 1} {:b 2} ⊚{:c 3}]"                          "[{:a 1} {:b 2}] ⊚{:c 3}"]
               ["[⊚1 ;; comment\n2]"                               "[⊚1];; comment\n2"]
               ["[1 ⊚;; comment\n2]"                               "[1];; comment\n⊚2"]
               ["[1 ;; comment\n⊚2]"                               "[1];; comment\n⊚2"]
               ["[1 ;; comment\n⊚2]"                               "[1];; comment\n⊚2"]
               ["[1 ;; cmt1\n;; cmt2\n⊚2]"                         "[1];; cmt1\n;; cmt2\n⊚2"]
               ["[1 \n   \n;; cmt1\n  \n;; cmt2\n   \n\n  ⊚2]"     "[1]\n\n;; cmt1\n\n;; cmt2\n\n\n⊚2"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
            (is (= expected (-> zloc pe/barf-forward th/root-locmarked-string)) "root string after")))))))

(deftest barf-backward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                                                   expected]
              [["[1 [2 ⊚3 4]]"                                    "[1 2 [⊚3 4]]"]
               ["[1 [2 3 ⊚4]]"                                    "[1 2 [3 ⊚4]]"]
               ["[1 [⊚2 3 4]]"                                    "[1 ⊚2 [3 4]]"]
               ["[1 [2⊚ 3 4]]"                                    "[1 ⊚2 [3 4]]"]
               ["[1 [⊚ 2 3 4]]"                                   "[1 ⊚2 [3 4]]"]
               ["[1 [⊚2]]"                                        "[1 ⊚2 []]"]
               ["(1 ⊚(x))"                                        "1 (⊚(x))"]
               ["(1⊚(x))"                                         "1 (⊚(x))"]
               ["((x)⊚(y))"                                       "(x) (⊚(y))"]
               ["[{:a 1} {:b 2} ⊚{:c 3}]"                         "{:a 1} [{:b 2} ⊚{:c 3}]"]
               ["[{:a 1} ⊚{:b 2} {:c 3}]"                         "{:a 1} [⊚{:b 2} {:c 3}]"]
               ["[⊚{:a 1} {:b 2} {:c 3}]"                         "⊚{:a 1} [{:b 2} {:c 3}]"]
               ["[1 ;; comment\n⊚2]"                              "1 ;; comment\n[⊚2]"]
               ["[1 ⊚;; comment\n2]"                              "⊚1 ;; comment\n[2]"]
               ["[⊚1 ;; comment\n2]"                              "⊚1 ;; comment\n[2]"]
               ["[⊚1 ;; cmt1\n;; cmt2\n2]"                        "⊚1 ;; cmt1\n;; cmt2\n[2]"]
               ["[⊚1 \n   \n;; cmt1\n  \n;; cmt2\n   \n\n  2]"    "⊚1 \n\n;; cmt1\n\n;; cmt2\n\n\n[2]"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
            (is (= expected (-> zloc pe/barf-backward th/root-locmarked-string)) "root string after")))))))

(deftest wrap-around-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                 t           expected]
              [["⊚1"             :list       "(⊚1)"]
               ["⊚1"             :vector     "[⊚1]"]
               ["⊚1"             :map        "{⊚1}"]
               ["⊚1"             :set        "#{⊚1}"]
               ["[⊚1\n 2]"       :vector     "[[⊚1]\n 2]"]
               ["(-> ⊚#(+ 1 1))" :list       "(-> (⊚#(+ 1 1)))"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc (pe/wrap-around t) th/root-locmarked-string)) "string after"))))))

(deftest wrap-fully-forward-slurp-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s               t           expected]
              [["[1 ⊚2 3 4]"   :vector     "[1 [⊚2 3 4]]"]
               ["[1 ⊚2 3 4]"   :map        "[1 {⊚2 3 4}]"]
               ["[1 ⊚2 3 4]"   :list       "[1 (⊚2 3 4)]"]
               ["[1 ⊚2 3 4]"   :set        "[1 #{⊚2 3 4}]"]
               ["[1 ⊚2]"       :list       "[1 (⊚2)]"]
               ["⊚[]"          :list       "(⊚[])"]]]
        (testing s
          (let [zloc (th/of-locmarked-string s opts)]
            (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
            (is (= expected (-> zloc (pe/wrap-fully-forward-slurp t) th/root-locmarked-string)) "string after")))))))

(deftest splice-killing-backward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> (th/of-locmarked-string "(foo (let ((x 5)) ⊚(sqrt n)) bar)" opts)
                    pe/splice-killing-backward)]
        (is (= "(foo ⊚(sqrt n) bar)" (th/root-locmarked-string res)))))))

(deftest splice-killing-forward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                          expected]
              [["(a (b c ⊚d e) f)"        "(a b ⊚c f)"]
               ["(a (⊚b c d e) f)"        "(⊚a f)"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc pe/splice-killing-forward th/root-locmarked-string)) "string after"))))))

(deftest split-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                                       expected]
              [["[⊚1 2]"                               "[⊚1] [2]"]
               ["[1 ⊚2 3 4]"                           "[1 ⊚2] [3 4]"]
               ["[1 2⊚ 3 4]"                           "[1 ⊚2] [3 4]"]
               ["[⊚1]"                                 "[⊚1]"] ;; no-op
               ["[⊚1 ;dill\n]"                         "[⊚1 ;dill\n]"] ;; no-op
               ["\n[1 ;dill\n ⊚2 ;dall\n 3 ;jalla\n]"  "\n[1 ;dill\n ⊚2 ;dall\n] [3 ;jalla\n]"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc pe/split th/root-locmarked-string)) "string after"))))))

(deftest split-at-pos-test
  ;; for this pos fn test, ⊚ in `s` represents character row/col the the `pos`
  ;; ⊚ in `expected` is at zipper node granularity
  (doseq [[s                                       expected]
          [["(\"Hello ⊚World\" 42)"                "(⊚\"Hello \" \"World\" 42)"]
           ["(\"⊚Hello World\" 101)"               "(⊚\"\" \"Hello World\" 101)"]
           ["(\"H⊚ello World\" 101)"               "(⊚\"H\" \"ello World\" 101)"]
           ["(⊚\"Hello World\" 101)"               "(⊚\"Hello World\") (101)"]]]
    (let [{:keys [pos s]} (th/pos-and-s s)
          zloc (z/of-string* s {:track-position? true})]
      (doseq [pos [pos [(:row pos) (:col pos)]]]
        (testing (str s " @pos " pos)
          (is (= expected (-> zloc (pe/split-at-pos pos) th/root-locmarked-string))))))))

(deftest join-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                                                            expected]
              [["[1 2]⊚ [3 4]"                                              "[1 2 ⊚3 4]"]
               ["#{1 2} ⊚[3 4]"                                             "#{1 2 ⊚3 4}"]
               ["(1 2)⊚ {3 4}"                                              "(1 2 ⊚3 4)"]
               ["{:a 1} ⊚(:b 2)"                                            "{:a 1 ⊚:b 2}"]
               ["[foo]⊚[bar]"                                               "[foo ⊚bar]"]
               ["[foo]   ⊚[bar]"                                            "[foo   ⊚bar]"]
               ["\n[[1 2]⊚ ; the first stuff\n [3 4] ; the second stuff\n]" "\n[[1 2 ; the first stuff\n ⊚3 4] ; the second stuff\n]"]
               ;; strings
               ["(\"Hello \" ⊚\"World\")"                                   "(⊚\"Hello World\")"]
               ["(⊚\"Hello \" \"World\")" "(⊚\"Hello \" \"World\")"]
               ["(\"Hello \" ;; comment\n;; comment2\n⊚\"World\")"
                "(⊚\"Hello World\" ;; comment\n;; comment2\n)"]
               ["\"foo\"⊚\"bar\""         "⊚\"foobar\""]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc pe/join th/root-locmarked-string)) "string after"))))))

(deftest raise-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[1 ⊚3]"
             (-> (th/of-locmarked-string "[1 [2 ⊚3 4]]" opts)
                 pe/raise
                 th/root-locmarked-string))))))

(deftest move-to-prev-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                          expected]
              [["(+ 1 ⊚2)"                "(+ ⊚2 1)"]
               ["(+ 1 (+ 2 3) ⊚4)"        "(+ 1 (+ 2 3 ⊚4))"]
               ["(+ 1 (+ 2 3 ⊚4))"        "(+ 1 (+ 2 ⊚4 3))"]
               ["(+ 1 (+ 2 ⊚4 3))"        "(+ 1 (+ ⊚4 2 3))"]
               ["(+ 1 (+ ⊚4 2 3))"        "(+ 1 (⊚4 + 2 3))"]
               ["(+ 1 (⊚4 + 2 3))"        "(+ 1 ⊚4 (+ 2 3))"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc pe/move-to-prev th/root-locmarked-string)) "string after"))))))
