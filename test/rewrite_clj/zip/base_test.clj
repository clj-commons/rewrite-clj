(ns rewrite-clj.zip.base-test
  (:require [clojure.test :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.custom-zipper.core :as z]))

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
      (base/of-file (.getPath f)))))

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
