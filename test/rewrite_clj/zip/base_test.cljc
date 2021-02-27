(ns rewrite-clj.zip.base-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as z]))

(let [n (node/forms-node
         [(node/spaces 3)
          (node/coerce [[1 2] 3])])
      s "   [[1 2] 3]"]
  (deftest t-edn-for-zipper-creation-from-node
    (let [loc (z/edn* n)
          [_ a b c d] (iterate z/next* loc)]
      (is (= :forms (z/tag loc)))
      (is (= [[1 2] 3] (z/sexpr loc)))
      (is (= :whitespace (z/tag a)))
      (is (= :vector (z/tag b)))
      (is (= :vector (z/tag c)))
      (is (= :token (z/tag d)))
      (is (= s (z/string loc)))
      (is (= "[1 2]" (z/string c)))
      (is (= s (with-out-str (z/print loc))))
      (is (= s (with-out-str (z/print-root loc))))
      (is (every? #{s} (map z/root-string [loc a b c d]))))))

#?(:clj
   (let [f (java.io.File/createTempFile "rewrite" ".clj")
         s "   [[1 2] 3]"]
     (spit f s)
     (deftest t-zipper-creation-with-movement-to-first-non-ws-node
       (are [?loc]
            (let [loc ?loc
                  [_ a b c d] (iterate z/next* loc)]
              (is (= :vector (z/tag loc)))
              (is (= [[1 2] 3] (z/sexpr loc)))
              (is (= :vector (z/tag a)))
              (is (= :token (z/tag b)))
              (is (= :whitespace (z/tag c)))
              (is (= :token (z/tag d)))
              (is (= "[[1 2] 3]" (z/string loc)))
              (is (= "[1 2]" (z/string a)))
              (is (every? #{s} (map z/root-string [loc a b c d])))
              (is (= "[[1 2] 3]" (with-out-str (z/print loc))))
              (is (= "   [[1 2] 3]" (with-out-str (z/print-root loc))))
              (is (= "[[1 2] 3]"
                     (with-open [w (java.io.StringWriter.)]
                       (z/print loc w)
                       (str w))))
              (is (= "   [[1 2] 3]"
                     (with-open [w (java.io.StringWriter.)]
                       (z/print-root loc w)
                       (str w)))))
         (z/edn
          (node/forms-node
           [(node/spaces 3)
            (node/coerce [[1 2] 3])]))
         (z/of-string s)
         (z/of-file f)
         (z/of-file (.getPath f))))))

(deftest t-zipper-creation-for-whitespace-only-nodes
  (are [?ws ?s]
       (let [n (node/forms-node [?ws])
             loc (z/edn n)]
         (is (= :forms (z/tag loc)))
         (is (= nil (z/sexpr loc)))
         (is (= ?s (z/string loc)))
         (is (= ?s (z/root-string loc))))
    (node/spaces 3)            "   "
    (node/comment-node "foo")  ";foo"))

(deftest t-length-calculation
  (are [?s]
       (let [s ?s
             loc (z/of-string s)]
         (is (= (count s) (z/length loc))))
    "0"
    "^:private x"
    "[1 2 [3 4] #{5}]"
    "{:a 0, :b 1, ::c 3, :ns/x}"
    "#inst \"2014\""
    "#_(+ 2 3)"
    "@(deref x)"
    "#=(+ 1 2)"))

(deftest t-sexpr-applies-auto-resolve-opts-to-nested-elements
  (are [?sexpr-default ?child-sexprs-default ?sexpr-custom ?child-sexprs-custom]
       (let [s "{:x [[[::a]] #{::myalias2/b} #::myalias{:x 1 :y 2}]}"
             zloc (z/of-string s)
             opts {:auto-resolve #(if (= :current %)
                                    'my.current.ns
                                    (get {'myalias 'my.aliased.ns} %
                                         (symbol (str % "-unresolved"))))}
             zloc-custom-opts (z/of-string s opts)]
         (is (= ?sexpr-default (z/sexpr zloc)))
         (is (= ?child-sexprs-default (z/child-sexprs zloc)))
         (is (= ?sexpr-custom (z/sexpr zloc-custom-opts)))
         (is (= ?child-sexprs-custom (z/child-sexprs zloc-custom-opts))))
    {:x [[[:?_current-ns_?/a]]
         #{:??_myalias2_??/b}
         {:??_myalias_??/x 1, :??_myalias_??/y 2}]}

    '(:x [[[:?_current-ns_?/a]]
          #{:??_myalias2_??/b}
          {:??_myalias_??/x 1, :??_myalias_??/y 2}])

    {:x [[[:my.current.ns/a]]
         #{:myalias2-unresolved/b}
         {:my.aliased.ns/x 1, :my.aliased.ns/y 2}]}

    '(:x [[[:my.current.ns/a]]
          #{:myalias2-unresolved/b}
          {:my.aliased.ns/x 1, :my.aliased.ns/y 2}])))
