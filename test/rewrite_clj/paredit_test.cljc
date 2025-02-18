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
              [["[1⊚ 2 3 4]"                 "[⊚1]"]
               ["[1 2]⊚ ; useless comment"   "⊚[1 2]"]
               ["[⊚1 2 3 4]"                 "⊚[]"]
               ["[⊚[1 2 3 4]]"               "⊚[]"]
               ["[1 2 3 4]⊚ 2"               "⊚[1 2 3 4]"]
               ["⊚[1 2 3 4] 5"               "◬"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) before changes")
          (is (= expected (-> zloc pe/kill th/root-locmarked-string))))))))

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
      (doseq [[s                                       expected]
              [["[[1 ⊚2] 3 4]"                         "[[1 ⊚2 3] 4]"]
               ["[[⊚1 2] 3 4]"                         "[[⊚1 2 3] 4]"]
               ["[⊚[] 1 2 3]"                          "[[⊚1] 2 3]"]
               ["[[1⊚ 2] 3 4]"                         "[[1⊚ 2 3] 4]"]
               ["[[[⊚1 2]] 3 4]"                       "[[[⊚1 2] 3] 4]"]
               ["[[[[[⊚1 2]]]] 3 4]"                   "[[[[[⊚1 2]]] 3] 4]"]
               ["[1 [⊚2 [3 4]] 5]"                     "[1 [⊚2 [3 4] 5]]"]
               ["\n(let [⊚dill]\n  {:a 1}\n  {:b 2})"  "\n(let [⊚dill \n{:a 1}]\n  {:b 2})"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) before changes")
          (is (= expected (-> zloc pe/slurp-forward th/root-locmarked-string))))))))

(deftest slurp-forward-fully-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[1 [⊚2 3 4]]" (-> (th/of-locmarked-string "[1 [⊚2] 3 4]" opts)
                                pe/slurp-forward-fully
                                th/root-locmarked-string))))))

(deftest slurp-backward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                                      expected]
              [["[1 2 [⊚3 4]]"                        "[1 [2 ⊚3 4]]"]
               ["[1 2 [3 ⊚4]]"                        "[1 [2 3 ⊚4]]"]
               ["[1 2 3 4 ⊚[]]"                       "[1 2 3 [⊚4]]"]
               ["[1 2 [[3 ⊚4]]]"                      "[1 [2 [3 ⊚4]]]"
                ["[1 2 [[[3 ⊚4]]]]"                    "[1 [2 [[3 ⊚4]]]]"]
                ["[1 2 ;dill\n [⊚3 4]]"                "[1 [2 ;dill\n ⊚3 4]]"]]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) before changes")
          (is (= expected (-> zloc pe/slurp-backward th/root-locmarked-string))))))))

(deftest slurp-backward-fully-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[[1 2 3 ⊚4] 5]" (-> (th/of-locmarked-string "[1 2 3 [⊚4] 5]" opts)
                                  pe/slurp-backward-fully
                                  th/root-locmarked-string))))))

(deftest barf-forward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                          expected]
              [["[[⊚1 2 3] 4]"            "[[⊚1 2] 3 4]"]
               ["[[1 ⊚2 3] 4]"            "[[1 ⊚2] 3 4]"]
               ["[[1 2 ⊚3] 4]"            "[[1 2] ⊚3 4]"]
               ["[[1 2 3⊚ ] 4]"           "[[1 2] ⊚3 4]"]
               ["[[⊚1] 2]"                "[[] ⊚1 2]"]
               ["(⊚(x) 1)"                "(⊚(x)) 1"]
               ["(⊚(x)1)"                 "(⊚(x)) 1"]
               ["(⊚(x)(y))"               "(⊚(x)) (y)"]
               ["[⊚{:a 1} {:b 2} {:c 3}]" "[⊚{:a 1} {:b 2}] {:c 3}"]
               ["[{:a 1} ⊚{:b 2} {:c 3}]" "[{:a 1} ⊚{:b 2}] {:c 3}"]
               ["[{:a 1} {:b 2} ⊚{:c 3}]" "[{:a 1} {:b 2}] ⊚{:c 3}"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "string before")
          (is (= expected (-> zloc pe/barf-forward th/root-locmarked-string)) "string after"))))))

(deftest barf-backward-test
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                          expected]
              [["[1 [2 3 ⊚4]]"            "[1 2 [3 ⊚4]]"]
               ["[1 [⊚2 3 4]]"            "[1 ⊚2 [3 4]]"]]]
        (let [zloc (th/of-locmarked-string s opts)]
          (is (= s (th/root-locmarked-string zloc)) "(sanity) string before")
          (is (= expected (-> zloc pe/barf-backward th/root-locmarked-string)) "string after"))))))

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
      (is (= "[1 [⊚2 3 4]]"
             (-> (th/of-locmarked-string "[1 ⊚2 3 4]" opts)
                 (pe/wrap-fully-forward-slurp :vector)
                 th/root-locmarked-string))))))

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
