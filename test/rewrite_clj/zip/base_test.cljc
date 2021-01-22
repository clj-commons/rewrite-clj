(ns rewrite-clj.zip.base-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]))

(let [n (node/forms-node
         [(node/spaces 3)
          (node/coerce [[1 2] 3])])
      s "   [[1 2] 3]"]
  (deftest t-edn-for-zipper-creation-from-node
    (let [loc (base/edn* n)
          [_ a b c d] (iterate z/next loc)]
      (is (= :forms (base/tag loc)))
      (is (= [[1 2] 3] (base/sexpr loc)))
      (is (= :whitespace (base/tag a)))
      (is (= :vector (base/tag b)))
      (is (= :vector (base/tag c)))
      (is (= :token (base/tag d)))
      (is (= s (base/string loc)))
      (is (= "[1 2]" (base/string c)))
      (is (every? #{s} (map base/root-string [loc a b c d]))))))

#?(:clj
   (let [f (java.io.File/createTempFile "rewrite" ".clj")
         s "   [[1 2] 3]"]
     (spit f s)
     (deftest t-zipper-creation-with-movement-to-first-non-ws-node
       (are [?loc]
            (let [loc ?loc
                  [_ a b c d] (iterate z/next loc)]
              (is (= :vector (base/tag loc)))
              (is (= [[1 2] 3] (base/sexpr loc)))
              (is (= :vector (base/tag a)))
              (is (= :token (base/tag b)))
              (is (= :whitespace (base/tag c)))
              (is (= :token (base/tag d)))
              (is (= "[[1 2] 3]" (base/string loc)))
              (is (= "[1 2]" (base/string a)))
              (is (every? #{s} (map base/root-string [loc a b c d])))
              (is (= "[[1 2] 3]" (with-out-str (base/print loc))))
              (is (= "   [[1 2] 3]" (with-out-str (base/print-root loc))))
              (is (= "[[1 2] 3]"
                     (with-open [w (java.io.StringWriter.)]
                       (base/print loc w)
                       (str w))))
              (is (= "   [[1 2] 3]"
                     (with-open [w (java.io.StringWriter.)]
                       (base/print-root loc w)
                       (str w)))))
         (base/edn
          (node/forms-node
           [(node/spaces 3)
            (node/coerce [[1 2] 3])]))
         (base/of-string s)
         (base/of-file f)
         (base/of-file (.getPath f))))))

(deftest t-zipper-creation-for-whitespace-only-nodes
  (are [?ws ?s]
       (let [n (node/forms-node [?ws])
             loc (base/edn n)]
         (is (= :forms (base/tag loc)))
         (is (= nil (base/sexpr loc)))
         (is (= ?s (base/string loc)))
         (is (= ?s (base/root-string loc))))
    (node/spaces 3)            "   "
    (node/comment-node "foo")  ";foo"))

(deftest t-length-calculation
  (are [?s]
       (let [s ?s
             loc (base/of-string s)]
         (is (= (count s) (base/length loc))))
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
             zloc (base/of-string s)
             opts {:auto-resolve #(if (= :current %)
                                    'my.current.ns
                                    (get {'myalias 'my.aliased.ns} %
                                         (symbol (str % "-unresolved"))))}
             zloc-custom-opts (base/of-string s opts)]
         (is (= ?sexpr-default (base/sexpr zloc)))
         (is (= ?child-sexprs-default (base/child-sexprs zloc)))
         (is (= ?sexpr-custom (base/sexpr zloc-custom-opts)))
         (is (= ?child-sexprs-custom (base/child-sexprs zloc-custom-opts))))
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
