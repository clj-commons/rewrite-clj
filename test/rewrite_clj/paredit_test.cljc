(ns rewrite-clj.paredit-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.paredit :as pe]
            [rewrite-clj.zip.test-helper :as th]))

;; special positional markers recognized by test-helper fns
;; ⊚ - node location
;; ◬ - root :forms node

(defn- move-n [loc f n]
  (->> loc (iterate f) (take n) last))

(def zipper-opts [{} {:track-position? true}])

(defn- zipper-opts-desc [opts]
  (str "zipper opts " opts))

(deftest kill-to-end-of-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1⊚ 2 3 4]"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "[⊚1]" (th/root-locmarked-string res)))))))

(deftest kill-to-end-of-line
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2]⊚ ; useless comment"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "⊚[1 2]" (th/root-locmarked-string res)))))))

(deftest kill-to-wipe-all-sexpr-contents
  (doseq [opts [{}] #_zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[⊚1 2 3 4]"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "⊚[]" (th/root-locmarked-string res)))))))

(deftest kill-to-wipe-all-sexpr-contents-in-nested-seq
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[⊚[1 2 3 4]]"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "⊚[]" (th/root-locmarked-string res)))))))

(deftest kill-when-left-is-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 3 4]⊚ 2"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "⊚[1 2 3 4]" (th/root-locmarked-string res)))))))

(deftest kill-it-all
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "⊚[1 2 3 4] 5"
                    (th/of-locmarked-string opts)
                    pe/kill)]
        (is (= "◬" (th/root-locmarked-string res)))))))

(deftest kill-at-pos-when-in-empty-seq
  (let [res (-> "⊚[] 5"
                (th/of-locmarked-string {:track-position? true})
                (pe/kill-at-pos {:row 1 :col 2}))]
    ;; TODO: questionable, our pos is now at :forms root node
    (is (= "◬5" (th/root-locmarked-string res)))))

(deftest kill-inside-comment
  (let [res (-> "⊚; dilldall"
                (th/of-locmarked-string {:track-position? true})
                (pe/kill-at-pos {:row 1 :col 7}))]
    (is (= "⊚; dill" (th/root-locmarked-string res)))))

(deftest kill-at-pos-when-string
  (let [res (-> "(⊚str \"Hello \" \"World!\")"
                (th/of-locmarked-string {:track-position? true})
                (pe/kill-at-pos {:row 1 :col 9}))]
    (is (= "(str ⊚\"He\" \"World!\")" (th/root-locmarked-string res)))))

(deftest kill-at-pos-when-string-multiline
  (let [sample "(⊚str \"
First line
  Second Line
    Third Line
        \")"
        expected "(str ⊚\"
First line
  Second\")"
        res (-> (th/of-locmarked-string sample {:track-position? true})
                (pe/kill-at-pos {:row 3 :col 9}))]
    (is (= expected (th/root-locmarked-string res)))))

(deftest kill-at-pos-multiline-aligned
  (let [sample "
⊚(println \"Hello
         There
         World\")"]
    (is (= "\n(println ⊚\"Hello\")" (-> (th/of-locmarked-string sample {:track-position? true})
                                        (pe/kill-at-pos {:row 2 :col 16})
                                        (th/root-locmarked-string))))))

(deftest kill-at-pos-when-empty-string
  (is (= "◬" (-> (th/of-locmarked-string "⊚\"\"" {:track-position? true})
                 (pe/kill-at-pos {:row 1 :col 1})
                 th/root-locmarked-string))))

(deftest kill-one-at-pos
  (let [sample "⊚[10 20 30]"]
    (is (= "[⊚10 30]"
           (-> (th/of-locmarked-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 4}) ; at whitespace before 20
               th/root-locmarked-string)))
    (is (= "[⊚10 30]"
           (-> (th/of-locmarked-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 5})
               th/root-locmarked-string)))))

(deftest kill-one-at-pos-new-zloc-is-left-node
  (let [sample "⊚[[10] 20 30]"]
    (is (= "[⊚[10] 30]"
           (-> (th/of-locmarked-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 6})
               th/root-locmarked-string)))
    (is (= "[⊚[10] 30]"
           (-> (th/of-locmarked-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 7})
               th/root-locmarked-string)))))

(deftest kill-one-at-pos-keep-linebreaks
  (let [sample (th/of-locmarked-string "⊚[10\n 20\n 30]" {:track-position? true})]
    (is (= "⊚[20\n 30]"
           (-> sample (pe/kill-one-at-pos {:row 1 :col 2}) th/root-locmarked-string)))
    (is (= "[⊚10\n 30]"
           (-> sample (pe/kill-one-at-pos {:row 2 :col 1}) th/root-locmarked-string)))
    (is (= "[10\n ⊚20]"
           (-> sample (pe/kill-one-at-pos {:row 3 :col 1}) th/root-locmarked-string)))))

(deftest kill-one-at-pos-in-comment
  (let [sample (th/of-locmarked-string "⊚; hello world" {:track-position? true})]
    (is (= "⊚; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 8}) th/root-locmarked-string)))
    (is (= "⊚; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 9}) th/root-locmarked-string)))
    (is (= "⊚; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 13}) th/root-locmarked-string)))
    (is (= "⊚;  world"
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) th/root-locmarked-string)))))

(deftest kill-one-at-pos-in-string
  (let [sample (th/of-locmarked-string "⊚\"hello world\"" {:track-position? true})]
    (is (= "⊚\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 7}) th/root-locmarked-string)))
    (is (= "⊚\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 8}) th/root-locmarked-string)))
    (is (= "⊚\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 12}) th/root-locmarked-string)))
    (is (= "⊚\" world\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) th/root-locmarked-string)))))

(deftest kill-one-at-pos-in-multiline-string
  (let [sample (th/of-locmarked-string "⊚\"foo bar do\n lorem\"" {:track-position? true})]
    (is (= "⊚\" bar do\n lorem\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) th/root-locmarked-string)))
    (is (= "⊚\"foo bar do\n \""
           (-> (pe/kill-one-at-pos sample {:row 2 :col 1}) th/root-locmarked-string)))
    (is (= "⊚\"foo bar \n lorem\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 10}) th/root-locmarked-string)))))

(deftest slurp-forward-and-keep-loc-rightmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[1 ⊚2] 3 4]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[1 ⊚2 3] 4]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-and-keep-loc-leftmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[⊚1 2] 3 4]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[⊚1 2 3] 4]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-from-empty-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[⊚[] 1 2 3]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[⊚1] 2 3]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-from-whitespace-node
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[1⊚ 2] 3 4]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[1⊚ 2 3] 4]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-nested
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[[⊚1 2]] 3 4]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[[⊚1 2] 3] 4]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-nested-silly
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[[[[⊚1 2]]]] 3 4]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[[[[[⊚1 2]]] 3] 4]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-when-last-is-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 [⊚2 [3 4]] 5]"
                    (th/of-locmarked-string opts)
                    pe/slurp-forward)]
        (is (= "[1 [⊚2 [3 4] 5]]" (th/root-locmarked-string res)))))))

(deftest slurp-forward-keep-linebreak
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [sample "
(let [⊚dill]
  {:a 1}
  {:b 2})"
            expected "\n(let [⊚dill \n{:a 1}]\n  {:b 2})"]
        (is (= expected (-> sample
                            (th/of-locmarked-string opts)
                            pe/slurp-forward
                            th/root-locmarked-string)))))))

(deftest slurp-forward-fully
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[1 [⊚2 3 4]]" (-> (th/of-locmarked-string "[1 [⊚2] 3 4]" opts)
                                pe/slurp-forward-fully
                                th/root-locmarked-string))))))

(deftest slurp-backward-and-keep-loc-leftmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 [⊚3 4]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 [2 ⊚3 4]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-and-keep-loc-rightmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 [3 ⊚4]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 [2 3 ⊚4]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-from-empty-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 3 4 ⊚[]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 2 3 [⊚4]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-nested
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 [[3 ⊚4]]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 [2 [3 ⊚4]]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-nested-silly
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 [[[3 ⊚4]]]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 [2 [[3 ⊚4]]]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-keep-linebreaks-and-comments
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2 ;dill\n [⊚3 4]]"
                    (th/of-locmarked-string opts)
                    pe/slurp-backward)]
        (is (= "[1 [2 ;dill\n ⊚3 4]]" (th/root-locmarked-string res)))))))

(deftest slurp-backward-fully
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[[1 2 3 ⊚4] 5]" (-> (th/of-locmarked-string "[1 2 3 [⊚4] 5]" opts)
                                  pe/slurp-backward-fully
                                  th/root-locmarked-string))))))

(deftest barf-forward-and-keep-loc
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[1 ⊚2 3] 4]"
                    (th/of-locmarked-string opts)
                    pe/barf-forward)]
        (is (= "[[1 ⊚2] 3 4]" (th/root-locmarked-string res)))))))

(deftest barf-forward-on-elem-with-children
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (doseq [[s                          expected]
              [["(⊚(x) 1)"                "(⊚(x)) 1"]
               ["(⊚(x)1)"                 "(⊚(x)) 1"]
               ["(⊚(x)(y))"               "(⊚(x)) (y)"]
               ["[⊚{:a 1} {:b 2} {:c 3}]" "[⊚{:a 1} {:b 2}] {:c 3}"]
               ["[{:a 1} ⊚{:b 2} {:c 3}]" "[{:a 1} ⊚{:b 2}] {:c 3}"]
               ["[{:a 1} {:b 2} ⊚{:c 3}]" "[{:a 1} {:b 2}] ⊚{:c 3}"]]]
        (let [zloc  (th/of-locmarked-string s opts)
              res   (pe/barf-forward zloc)]
          (is (= s (th/root-locmarked-string zloc)) "string before")
          (is (= expected (th/root-locmarked-string res)) "string after"))))))

(deftest barf-forward-at-leftmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[⊚1 2 3] 4]"
                    (th/of-locmarked-string opts)
                    pe/barf-forward)]
        (is (= "[[⊚1 2] 3 4]" (th/root-locmarked-string res)))))))

(deftest barf-forward-at-rightmost-moves-out-of-sexrp
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[1 2 ⊚3] 4]"
                    (th/of-locmarked-string opts)
                    pe/barf-forward)]
        (is (= "[[1 2] ⊚3 4]" (th/root-locmarked-string res)))))))

(deftest barf-forward-at-rightmost-which-is-a-whitespace-haha
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[1 2 3⊚ ] 4]"
                    (th/of-locmarked-string opts)
                    pe/barf-forward)]
        (is (= "[[1 2] ⊚3 4]" (th/root-locmarked-string res)))))))

(deftest barf-forward-at-when-only-one
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[[⊚1] 2]"
                    (th/of-locmarked-string opts)
                    pe/barf-forward)]
        (is (= "[[] ⊚1 2]" (th/root-locmarked-string res)))))))

(deftest barf-backward-and-keep-current-loc
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 [2 3 ⊚4]]"
                    (th/of-locmarked-string opts)
                    pe/barf-backward)]
        (is (= "[1 2 [3 ⊚4]]" (th/root-locmarked-string res)))))))

(deftest barf-backward-at-leftmost-moves-out-of-sexpr
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 [⊚2 3 4]]"
                    (th/of-locmarked-string opts)
                    pe/barf-backward)]
        (is (= "[1 ⊚2 [3 4]]" (th/root-locmarked-string res)))))))

(deftest wrap-around
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(⊚1)" (-> (th/of-locmarked-string "⊚1" opts) (pe/wrap-around :list) th/root-locmarked-string)))
      (is (= "[⊚1]" (-> (th/of-locmarked-string "⊚1" opts) (pe/wrap-around :vector) th/root-locmarked-string)))
      (is (= "{⊚1}" (-> (th/of-locmarked-string "⊚1" opts) (pe/wrap-around :map) th/root-locmarked-string)))
      (is (= "#{⊚1}" (-> (th/of-locmarked-string "⊚1" opts) (pe/wrap-around :set) th/root-locmarked-string))))))

;; TODO: now duplicate of above in wrap-around, will delete
(deftest wrap-around-keeps-loc
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "⊚1"
                    (th/of-locmarked-string opts)
                    (pe/wrap-around :list))]
        (is (= "(⊚1)" (th/root-locmarked-string res)))))))

(deftest wrap-around-keeps-newlines
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[[⊚1]\n 2]" (-> (th/of-locmarked-string "[⊚1\n 2]" opts)
                              (pe/wrap-around :vector)
                              th/root-locmarked-string))))))

(deftest wrap-around-fn
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(-> (⊚#(+ 1 1)))" (-> (th/of-locmarked-string "(-> ⊚#(+ 1 1))" opts)
                                   (pe/wrap-around :list)
                                   th/root-locmarked-string))))))

(deftest wrap-fully-forward-slurp
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[1 [⊚2 3 4]]"
             (-> (th/of-locmarked-string "[1 ⊚2 3 4]" opts)
                 (pe/wrap-fully-forward-slurp :vector)
                 th/root-locmarked-string))))))

(deftest splice-killing-backward
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> (th/of-locmarked-string "(foo (let ((x 5)) ⊚(sqrt n)) bar)" opts)
                    pe/splice-killing-backward)]
        (is (= "(foo ⊚(sqrt n) bar)" (th/root-locmarked-string res)))))))

(deftest splice-killing-forward
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> (th/of-locmarked-string "(a (b c ⊚d e) f)" opts)
                    pe/splice-killing-forward)]
        (is (= "(a b ⊚c f)" (th/root-locmarked-string res)))))))

(deftest splice-killing-forward-at-leftmost
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> (th/of-locmarked-string "(a (⊚b c d e) f)" opts)
                    pe/splice-killing-forward)]
        (is (= "(⊚a f)" (th/root-locmarked-string res)))))))

(deftest split
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[⊚1 2]"
                    (th/of-locmarked-string opts)
                    pe/split)]
        (is (= "[⊚1] [2]" (th/root-locmarked-string res)))))))

(deftest split-includes-node-at-loc-as-left
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 ⊚2 3 4]"
                    (th/of-locmarked-string opts)
                    pe/split)]
        (is (= "[1 ⊚2] [3 4]" (th/root-locmarked-string res)))))))

(deftest split-at-whitespace
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2⊚ 3 4]"
                    (th/of-locmarked-string opts)
                    pe/split)]
        (is (= "[1 ⊚2] [3 4]" (th/root-locmarked-string res)))))))

(deftest split-includes-comments-and-newlines
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [s "
[1 ;dill
 ⊚2 ;dall
 3 ;jalla
]"
            expected "
[1 ;dill
 ⊚2 ;dall
] [3 ;jalla
]"
            res (-> s
                    (th/of-locmarked-string opts)
                    pe/split)]
        (is (= expected (th/root-locmarked-string res)))))))

(deftest split-when-only-one-returns-self
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[⊚1]" (-> (th/of-locmarked-string "[⊚1]" opts)
                        pe/split
                        th/root-locmarked-string)))
      (is (= "[⊚1 ;dill\n]" (-> (th/of-locmarked-string "[⊚1 ;dill\n]" opts)
                                pe/split
                                th/root-locmarked-string))))))

(deftest split-at-pos-when-string
  (is (= "(⊚\"Hello \" \"World\")"
         (-> (th/of-locmarked-string "⊚(\"Hello World\")" {:track-position? true})
             (pe/split-at-pos {:row 1 :col 9})
             th/root-locmarked-string))))

(deftest join-simple
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [res (-> "[1 2]⊚ [3 4]"
                    (th/of-locmarked-string opts)
                    pe/join)]
        (is (= "[1 2 ⊚3 4]" (th/root-locmarked-string res)))))))

(deftest join-with-comments
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (let [s "
[[1 2]⊚ ; the first stuff
 [3 4] ; the second stuff
]"      expected "
[[1 2 ; the first stuff
 ⊚3 4]; the second stuff
]"
            res (-> s
                    (th/of-locmarked-string opts)
                    pe/join)]
        (is (= expected (th/root-locmarked-string res)))))))

(deftest join-strings
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(⊚\"Hello World\")" (-> (th/of-locmarked-string "(\"Hello \" ⊚\"World\")" opts)
                                      pe/join
                                      th/root-locmarked-string))))))

(deftest raise
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "[1 ⊚3]"
             (-> (th/of-locmarked-string "[1 [2 ⊚3 4]]" opts)
                 pe/raise
                 th/root-locmarked-string))))))

(deftest move-to-prev-flat
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(+ ⊚2 1)" (-> "(+ 1 ⊚2)"
                            (th/of-locmarked-string opts)
                            pe/move-to-prev
                            th/root-locmarked-string))))))

(deftest move-to-prev-when-prev-is-seq
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(+ 1 (+ 2 3 ⊚4))" (-> "(+ 1 (+ 2 3) ⊚4)"
                                   (th/of-locmarked-string opts)
                                   pe/move-to-prev
                                   th/root-locmarked-string))))))

(deftest move-to-prev-out-of-seq
  (doseq [opts zipper-opts]
    (testing (zipper-opts-desc opts)
      (is (= "(+ 1 ⊚4 (+ 2 3))" (-> "(+ 1 (+ 2 3) ⊚4)"
                                    (th/of-locmarked-string opts)
                                    (move-n pe/move-to-prev 6)
                                    th/root-locmarked-string))))))
