(ns rewrite-clj.paredit-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.paredit :as pe]
            [rewrite-clj.zip :as z]))

;; helper
(defn move-n [loc f n]
  (->> loc (iterate f) (take n) last))

(def zipper-opts [{} {:track-position? true}])

(deftest kill-to-end-of-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts " opts)
      (let [res (-> "[1 2 3 4]"
                    (z/of-string opts)
                    z/down z/right*
                    pe/kill)]
        (is (= "[1]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest kill-to-end-of-line
  (doseq [opts zipper-opts]
    (testing (str "opts " opts)
      (let [res (-> "[1 2] ; useless comment"
                    (z/of-string opts)
                    z/right*
                    pe/kill)]
        (is (= "[1 2]" (-> res z/root-string)))
        (is (= "[1 2]" (-> res z/string)))))))

(deftest kill-to-wipe-all-sexpr-contents
  (doseq [opts [{}] #_zipper-opts]
    (testing (str "opts " opts)
      (let [res (-> "[1 2 3 4]"
                    (z/of-string opts)
                    z/down
                    pe/kill)]
        (is (= "[]" (-> res z/root-string)))
        (is (= "[]" (-> res z/string)))))))

(deftest kill-to-wipe-all-sexpr-contents-in-nested-seq
  (doseq [opts zipper-opts]
    (testing (str "opts " opts)
      (let [res (-> "[[1 2 3 4]]"
                    (z/of-string opts)
                    z/down
                    pe/kill)]
        (is (= "[]" (-> res z/root-string)))
        (is (= "[]" (-> res z/string)))))))

(deftest kill-when-left-is-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 3 4] 2"
                    (z/of-string opts)
                    z/right*
                    pe/kill)]
        (is (= "[1 2 3 4]" (-> res z/root-string)))
        (is (= "[1 2 3 4]" (-> res z/string)))))))

(deftest kill-it-all
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 3 4] 5"
                    (z/of-string opts)
                    pe/kill)]
        (is (= "" (-> res z/root-string)))
        (is (= "" (-> res z/string)))))))

(deftest kill-at-pos-when-in-empty-seq
  (let [res (-> "[] 5"
                (z/of-string {:track-position? true})
                (pe/kill-at-pos {:row 1 :col 2}))]
    (is (= "5" (-> res z/root-string)))
    (is (= "5" (-> res z/string)))))

(deftest kill-inside-comment
  (is (= "; dill" (-> "; dilldall"
                      (z/of-string {:track-position? true})
                      (pe/kill-at-pos {:row 1 :col 7})
                      z/root-string))))

(deftest kill-at-pos-when-string
  (let [res (-> "(str \"Hello \" \"World!\")"
                (z/of-string {:track-position? true})
                z/down
                (pe/kill-at-pos {:row 1 :col 9}))]
    (is (= "(str \"He\" \"World!\")" (-> res z/root-string)))))

(deftest kill-at-pos-when-string-multiline
  (let [sample "(str \"
First line
  Second Line
    Third Line
        \")"
        expected "(str \"
First line
  Second\")"

        res (-> (z/of-string sample {:track-position? true})
                z/down
                (pe/kill-at-pos {:row 3 :col 9}))]
    (is (= expected (-> res z/root-string)))))

(deftest kill-at-pos-multiline-aligned
  (let [sample "
(println \"Hello
         There
         World\")"]
    (is (= "\n(println \"Hello\")" (-> (z/of-string sample {:track-position? true})
                                       (pe/kill-at-pos {:row 2 :col 16})
                                       (z/root-string))))))

(deftest kill-at-pos-when-empty-string
  (is (= "" (-> (z/of-string "\"\"" {:track-position? true})
                (pe/kill-at-pos {:row 1 :col 1}) z/root-string))))

(deftest kill-one-at-pos
  (let [sample "[10 20 30]" ]
    (is (= "[10 30]"
           (-> (z/of-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 4}) ; at whitespace
               z/root-string)))
    (is (= "[10 30]"
           (-> (z/of-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 5})
               z/root-string)))))

(deftest kill-one-at-pos-new-zloc-is-left-node
  (let [sample "[[10] 20 30]"]
    (is (= "[10]"
           (-> (z/of-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 6})
               z/string)))
    (is (= "[10]"
           (-> (z/of-string sample {:track-position? true})
               (pe/kill-one-at-pos {:row 1 :col 7})
               z/string)))))

(deftest kill-one-at-pos-keep-linebreaks
  (let [sample (z/of-string "[10\n 20\n 30]" {:track-position? true})]
    (is (= "[20\n 30]"
           (-> sample (pe/kill-one-at-pos {:row 1 :col 2}) z/root-string)))
    (is (= "[10\n 30]"
           (-> sample (pe/kill-one-at-pos {:row 2 :col 1}) z/root-string)))
    (is (= "[10\n 20]"
           (-> sample (pe/kill-one-at-pos {:row 3 :col 1}) z/root-string)))))

(deftest kill-one-at-pos-in-comment
  (let [sample (z/of-string "; hello world" {:track-position? true})]
    (is (= "; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 8}) z/root-string)))
    (is (= "; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 9}) z/root-string)))
    (is (= "; hello "
           (-> (pe/kill-one-at-pos sample {:row 1 :col 13}) z/root-string)))
    (is (= ";  world"
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) z/root-string)))))

(deftest kill-one-at-pos-in-string
  (let [sample (z/of-string "\"hello world\"" {:track-position? true})]
    (is (= "\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 7}) z/root-string)))
    (is (= "\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 8}) z/root-string)))
    (is (= "\"hello \""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 12}) z/root-string)))
    (is (= "\" world\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) z/root-string)))))


(deftest kill-one-at-pos-in-multiline-string
  (let [sample (z/of-string "\"foo bar do\n lorem\"" {:track-position? true})]
    (is (= "\" bar do\n lorem\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 2}) z/root-string)))
    (is (= "\"foo bar do\n \""
           (-> (pe/kill-one-at-pos sample {:row 2 :col 1}) z/root-string)))
    (is (= "\"foo bar \n lorem\""
           (-> (pe/kill-one-at-pos sample {:row 1 :col 10}) z/root-string)))))



(deftest slurp-forward-and-keep-loc-rightmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2] 3 4]"
                    (z/of-string opts)
                    z/down z/down z/right
                    pe/slurp-forward)]
        (is (= "[[1 2 3] 4]" (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest slurp-forward-and-keep-loc-leftmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2] 3 4]"
                    (z/of-string opts)
                    z/down z/down
                    pe/slurp-forward)]
        (is (= "[[1 2 3] 4]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest slurp-forward-from-empty-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[] 1 2 3]"
                    (z/of-string opts)
                    z/down
                    pe/slurp-forward)]
        (is (= "[[1] 2 3]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest slurp-forward-from-whitespace-node
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2] 3 4]"
                    (z/of-string opts)
                    z/down z/down z/right*
                    pe/slurp-forward)]
        (is (= "[[1 2 3] 4]" (-> res z/root-string)))
        (is (= " " (-> res z/string)))))))

(deftest slurp-forward-nested
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[[1 2]] 3 4]"
                    (z/of-string opts)
                    z/down z/down z/down
                    pe/slurp-forward)]
        (is (= "[[[1 2] 3] 4]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest slurp-forward-nested-silly
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[[[[1 2]]]] 3 4]"
                    (z/of-string opts)
                    z/down z/down z/down z/down z/down
                    pe/slurp-forward)]
        (is (= "[[[[[1 2]]] 3] 4]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest slurp-forward-when-last-is-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 [2 [3 4]] 5]"
                    (z/of-string opts)
                    z/down z/right z/down ;at 2
                    pe/slurp-forward)]
        (is (= "[1 [2 [3 4] 5]]" (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest slurp-forward-keep-linebreak
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [sample "
(let [dill]
  {:a 1}
  {:b 2})"
            expected "\n(let [dill \n{:a 1}]\n  {:b 2})"]
        (is (= expected (-> sample
                            (z/of-string opts)
                            z/down z/right z/down
                            pe/slurp-forward
                            z/root-string)))))))

(deftest slurp-forward-fully
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[1 [2 3 4]]" (-> (z/of-string "[1 [2] 3 4]" opts)
                               z/down z/right z/down
                               pe/slurp-forward-fully
                               z/root-string))))))

(deftest slurp-backward-and-keep-loc-leftmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 [3 4]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down
                    pe/slurp-backward)]
        (is (= "[1 [2 3 4]]" (-> res z/root-string)))
        (is (= "3" (-> res z/string)))))))

(deftest slurp-backward-and-keep-loc-rightmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 [3 4]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down z/rightmost
                    pe/slurp-backward)]
        (is (= "[1 [2 3 4]]" (-> res z/root-string)))
        (is (= "4" (-> res z/string)))))))

(deftest slurp-backward-from-empty-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 3 4 []]"
                    (z/of-string opts)
                    z/down z/rightmost
                    pe/slurp-backward)]
        (is (= "[1 2 3 [4]]" (-> res z/root-string)))
        (is (= "4" (-> res z/string)))))))

(deftest slurp-backward-nested
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 [[3 4]]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down z/down z/rightmost
                    pe/slurp-backward)]
        (is (= "[1 [2 [3 4]]]" (-> res z/root-string)))
        (is (= "4" (-> res z/string)))))))

(deftest slurp-backward-nested-silly
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 [[[3 4]]]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down z/down z/down z/rightmost
                    pe/slurp-backward)]
        (is (= "[1 [2 [[3 4]]]]" (-> res z/root-string)))
        (is (= "4" (-> res z/string)))))))

(deftest slurp-backward-keep-linebreaks-and-comments
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 ;dill\n [3 4]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down
                    pe/slurp-backward)]
        (is (= "[1 [2 ;dill\n 3 4]]" (-> res z/root-string)))))))

(deftest slurp-backward-fully
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[[1 2 3 4] 5]" (-> (z/of-string "[1 2 3 [4] 5]" opts)
                                 z/down z/rightmost z/left z/down
                                 pe/slurp-backward-fully
                                 z/root-string))))))

(deftest barf-forward-and-keep-loc
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
  (let [res (-> "[[1 2 3] 4]"
                    (z/of-string opts)
                z/down z/down z/right; position at 2
                pe/barf-forward)]
    (is (= "[[1 2] 3 4]" (-> res z/root-string)))
    (is (= "2" (-> res z/string)))))))

(deftest barf-forward-on-elem-with-children
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (doseq [[s next-count expected-string expected-root-string]
              [["((x) 1)"                1 "(x)"    "((x)) 1"]
               ["((x)1)"                 1 "(x)"    "((x)) 1"]
               ["((x)(y))"               1 "(x)"    "((x)) (y)"]
               ["[{:a 1} {:b 2} {:c 3}]" 1 "{:a 1}" "[{:a 1} {:b 2}] {:c 3}"]
               ["[{:a 1} {:b 2} {:c 3}]" 4 "{:b 2}" "[{:a 1} {:b 2}] {:c 3}"]
               ["[{:a 1} {:b 2} {:c 3}]" 7 "{:c 3}" "[{:a 1} {:b 2}] {:c 3}"]]]
        (let [root (z/of-string s opts)
              zloc-before (nth (iterate z/next root) next-count)
              res (pe/barf-forward zloc-before)]
          (is (= expected-string (z/string zloc-before)) "string before")
          (is (= expected-string (z/string res)) "string after")
          (is (= expected-root-string (z/root-string res)) "root-string after"))))))

(deftest barf-forward-at-leftmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2 3] 4]"
                    (z/of-string opts)
                    z/down z/down
                    pe/barf-forward)]
        (is (= "[[1 2] 3 4]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest barf-forward-at-rightmost-moves-out-of-sexrp
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2 3] 4]"
                    (z/of-string opts)
                    z/down z/down z/rightmost; position at 3
                    pe/barf-forward)]
        (is (= "[[1 2] 3 4]" (-> res z/root-string)))
        (is (= "3" (-> res z/string)))))))

(deftest barf-forward-at-rightmost-which-is-a-whitespace-haha
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1 2 3 ] 4]"
                    (z/of-string opts)
                    z/down z/down z/rightmost*; position at space at the end
                    pe/barf-forward)]
        (is (= "[[1 2] 3 4]" (-> res z/root-string)))
        (is (= "3" (-> res z/string)))))))

(deftest barf-forward-at-when-only-one
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[[1] 2]"
                    (z/of-string opts)
                    z/down z/down
                    pe/barf-forward)]
        (is (= "[[] 1 2]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest barf-backward-and-keep-current-loc
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 [2 3 4]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down z/rightmost ; position at 4
                    pe/barf-backward)]
        (is (= "[1 2 [3 4]]" (-> res z/root-string)))
        (is (= "4" (-> res z/string)))))))

(deftest barf-backward-at-leftmost-moves-out-of-sexpr
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 [2 3 4]]"
                    (z/of-string opts)
                    z/down z/rightmost z/down ; position at 2
                    pe/barf-backward)]
        (is (= "[1 2 [3 4]]" (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest wrap-around
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(1)" (-> (z/of-string "1" opts) (pe/wrap-around :list) z/root-string)))
      (is (= "[1]" (-> (z/of-string "1" opts) (pe/wrap-around :vector) z/root-string)))
      (is (= "{1}" (-> (z/of-string "1" opts) (pe/wrap-around :map) z/root-string)))
      (is (= "#{1}" (-> (z/of-string "1" opts) (pe/wrap-around :set) z/root-string))))))

(deftest wrap-around-keeps-loc
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "1"
                    (z/of-string opts)
                    (pe/wrap-around :list))]
        (is (= "1" (-> res z/string)))))))

(deftest wrap-around-keeps-newlines
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[[1]\n 2]" (-> (z/of-string "[1\n 2]" opts)
                             z/down
                             (pe/wrap-around :vector)
                             z/root-string))))))

(deftest wrap-around-fn
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(-> (#(+ 1 1)))" (-> (z/of-string "(-> #(+ 1 1))" opts)
                                   z/down z/right
                                   (pe/wrap-around :list)
                                   z/root-string))))))

(deftest wrap-fully-forward-slurp
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[1 [2 3 4]]"
             (-> (z/of-string "[1 2 3 4]" opts)
                 z/down z/right
                 (pe/wrap-fully-forward-slurp :vector)
                 z/root-string))))))

(deftest splice-killing-backward
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> (z/of-string "(foo (let ((x 5)) (sqrt n)) bar)" opts)
                    z/down z/right z/down z/right z/right
                    pe/splice-killing-backward)]
        (is (= "(foo (sqrt n) bar)" (z/root-string res)))
        (is (= "(sqrt n)" (z/string res)))))))

(deftest splice-killing-forward
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> (z/of-string "(a (b c d e) f)" opts)
                    z/down z/right z/down z/right z/right
                    pe/splice-killing-forward)]
        (is (= "(a b c f)" (z/root-string res)))
        (is (= "c" (z/string res)))))))

(deftest splice-killing-forward-at-leftmost
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> (z/of-string "(a (b c d e) f)" opts)
                    z/down z/right z/down
                    pe/splice-killing-forward)]
        (is (= "(a f)" (z/root-string res)))
        (is (= "a" (z/string res)))))))

(deftest split
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2]"
                    (z/of-string opts)
                    z/down
                    pe/split)]
        (is (= "[1] [2]" (-> res z/root-string)))
        (is (= "1" (-> res z/string)))))))

(deftest split-includes-node-at-loc-as-left
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 3 4]"
                    (z/of-string opts)
                    z/down z/right
                    pe/split)]
        (is (= "[1 2] [3 4]" (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest split-at-whitespace
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2 3 4]"
                    (z/of-string opts)
                    z/down z/right z/right*
                    pe/split)]
        (is (= "[1 2] [3 4]" (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest split-includes-comments-and-newlines
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [sexpr "
[1 ;dill
 2 ;dall
 3 ;jalla
]"
            expected "
[1 ;dill
 2 ;dall
] [3 ;jalla
]"
            res (-> sexpr
                    (z/of-string opts)
                    z/down z/right
                    pe/split)]
        (is (= expected (-> res z/root-string)))
        (is (= "2" (-> res z/string)))))))

(deftest split-when-only-one-returns-self
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[1]" (-> (z/of-string "[1]" opts)
                       z/down
                       pe/split
                       z/root-string)))
      (is (= "[1 ;dill\n]" (-> (z/of-string "[1 ;dill\n]" opts)
                               z/down
                               pe/split
                               z/root-string))))))

(deftest split-at-pos-when-string
  (is (= "(\"Hello \" \"World\")"
         (-> (z/of-string "(\"Hello World\")" {:track-position? true})
             (pe/split-at-pos {:row 1 :col 9})
             z/root-string))))


(deftest join-simple
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [res (-> "[1 2] [3 4]"
                    (z/of-string opts)
                ;z/down
                    z/right*
                    pe/join)]
        (is (= "[1 2 3 4]" (-> res z/root-string)))
        (is (= "3" (-> res z/string)))))))

(deftest join-with-comments
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (let [sexpr "
[[1 2] ; the first stuff
 [3 4] ; the second stuff
]"      expected "
[[1 2 ; the first stuff
 3 4]; the second stuff
]"
            res (-> sexpr
                    (z/of-string opts)
                    z/down z/right*
                    pe/join)]
        (is (= expected (-> res z/root-string)))))))

(deftest join-strings
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(\"Hello World\")" (-> (z/of-string "(\"Hello \" \"World\")" opts)
                                     z/down z/rightmost
                                     pe/join
                                     z/root-string))))))

(deftest raise
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "[1 3]"
             (-> (z/of-string "[1 [2 3 4]]" opts)
                 z/down z/right z/down z/right
                 pe/raise
                 z/root-string))))))

(deftest move-to-prev-flat
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(+ 2 1)" (-> "(+ 1 2)"
                           (z/of-string opts)
                           z/down
                           z/rightmost
                           pe/move-to-prev
                           z/root-string))))))

(deftest move-to-prev-when-prev-is-seq
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(+ 1 (+ 2 3 4))" (-> "(+ 1 (+ 2 3) 4)"
                                   (z/of-string opts)
                                   z/down
                                   z/rightmost
                                   pe/move-to-prev
                                   z/root-string))))))

(deftest move-to-prev-out-of-seq
  (doseq [opts zipper-opts]
    (testing (str "opts" opts)
      (is (= "(+ 1 4 (+ 2 3))" (-> "(+ 1 (+ 2 3) 4)"
                                   (z/of-string opts)
                                   z/down
                                   z/rightmost
                                   (move-n pe/move-to-prev 6)
                                   z/root-string))))))
