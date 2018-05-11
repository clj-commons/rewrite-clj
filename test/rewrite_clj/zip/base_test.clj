(ns rewrite-clj.zip.base-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.custom-zipper.core :as z]))

(let [n (node/forms-node
          [(node/spaces 3)
           (node/coerce [[1 2] 3])])
      s "   [[1 2] 3]"]
  (fact "about 'edn*' for zipper creation from node."
        (let [loc (base/edn* n)
              [_ a b c d] (iterate z/next loc)]
          (base/tag loc) => :forms
          (base/sexpr loc) => [[1 2] 3]
          (base/tag a) => :whitespace
          (base/tag b) => :vector
          (base/tag c) => :vector
          (base/tag d) => :token
          (base/string loc) => s
          (base/string c) => "[1 2]"
          (map base/root-string [loc a b c d]) => (has every? #{s}))))

(let [f (java.io.File/createTempFile "rewrite" ".clj")
      s "   [[1 2] 3]"]
  (spit f s)
  (tabular
    (fact "about zipper creation (with movement to first non-ws node)."
          (let [loc ?loc
                [_ a b c d] (iterate z/next loc)]
            (base/tag loc) => :vector
            (base/sexpr loc) => [[1 2] 3]
            (base/tag a) => :vector
            (base/tag b) => :token
            (base/tag c) => :whitespace
            (base/tag d) => :token
            (base/string loc) => "[[1 2] 3]"
            (base/string a) => "[1 2]"
            (map base/root-string [loc a b c d]) => (has every? #{s})
            (with-out-str (base/print loc)) => "[[1 2] 3]"
            (with-out-str (base/print-root loc)) => "   [[1 2] 3]"
            (with-open [w (java.io.StringWriter.)]
              (base/print loc w)
              (str w)) => "[[1 2] 3]"
            (with-open [w (java.io.StringWriter.)]
              (base/print-root loc w)
              (str w)) => "   [[1 2] 3]"))
    ?loc
    (base/edn
      (node/forms-node
        [(node/spaces 3)
         (node/coerce [[1 2] 3])]))
    (base/of-string s)
    (base/of-file f)
    (base/of-file (.getPath f))))

(tabular
  (fact "about zipper creation for whitespace-only nodes."
      (let [n (node/forms-node [?ws])
            loc (base/edn n)]
        (base/tag loc) => :forms
        (base/sexpr loc) => nil
        (base/string loc) => ?s
        (base/root-string loc) => ?s))
  ?ws                        ?s
  (node/spaces 3)            "   "
  (node/comment-node "foo")  ";foo")

(tabular
  (fact "about length calculation."
        (let [s ?s
              loc (base/of-string s)]
          (base/length loc) => (count s)))
  ?s
  "0"
  "^:private x"
  "[1 2 [3 4] #{5}]"
  "{:a 0, :b 1, ::c 3, :ns/x}"
  "#inst \"2014\""
  "#_(+ 2 3)"
  "@(deref x)"
  "#=(+ 1 2)")
