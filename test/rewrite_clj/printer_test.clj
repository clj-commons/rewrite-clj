(ns ^{:doc "Tests for EDN printer." 
      :author "Yannick Scherer"}
  rewrite-clj.printer-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.parser :refer [parse-string-all]]
            [rewrite-clj.printer :refer :all]))

(tabular
  (fact "about correct printing of EDN/Clojure"
    (let [tree (parse-string-all ?str)]
      (estimate-length tree) => (count ?str)
      (->string tree) => ?str))
  ?str
  "0"    "0.1"     "1N"
  ":key" ":ns/key" "::key"
  "sym"  "sym#"
  "\"string\""

  "@sym"  "#'sym" "'sym" "~sym"
  "~@sym" "`sym"  "#=sym"

  "(first form) (second form)"
  "[:complex (list {:map 0})]"
  "#=(eval this)" "#date s"

  "#\"regex\"" "#\"regex\\.\"" "#\"[reg|k].x\""

  "^:private s"
  "^{:private true} s"
  "#^:private s"
  "#^{:private true} ^:privates s"

  ";; Hi!\n(def pi 3.14)")
