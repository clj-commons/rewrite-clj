(ns ^{ :doc "Tests for EDN parser."
       :author "Yannick Scherer" }
  rewrite-clj.parser-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.parser :as p]))

(fact "about parsing whitespace"
  (p/parse-string "   ") => [:whitespace "   "]
  (p/parse-string "   \n   ") => [:whitespace "   "]
  (p/parse-string-all "   \n   ") 
      => [:forms [:whitespace "   "] [:newline "\n"] [:whitespace "   "]]
  (p/parse-string-all " \t \r\n \t ") 
      => [:forms [:whitespace " \t "] [:newline "\n\n"] [:whitespace " \t "]])

(tabular
  (fact "about parsing simple data"
    (p/parse-string ?s) => [:token ?r])
  ?s                           ?r
  "0"                          0
  "0.1"                        0.1
  "1N"                         1N
  ":key"                       :key
  "sym"                        'sym
  "sym#"                       'sym#
  "\"string\""                 "string")

(tabular
  (fact "about parsing prefixed data"
    (p/parse-string ?s) => [?t [:token ?r]])
  ?s                     ?t                ?r
  "@sym"                 :deref            'sym
  "'sym"                 :quote            'sym
  "`sym"                 :syntax-quote     'sym
  "~sym"                 :unquote          'sym
  "~@sym"                :unquote-splicing 'sym
  "#=sym"                :eval             'sym
  "#'sym"                :var              'sym)

(tabular
  (fact "about parsing regular expressions"
    (let [r (p/parse-string ?s)]
      (first r) => :token
      (class (second r)) => java.util.regex.Pattern
      (str (second r)) => ?p))
  ?s                 ?p 
  "#\"regex\""       "regex"
  "#\"regex\\.\""    "regex\\."
  "#\"[reg|k].x\""   "[reg|k].x")

(fact "about parsing strings"
  (p/parse-string "\"123\"") => [:token "123"]
  (p/parse-string "\"123\\n456\"") => [:token "123\n456"]
  (p/parse-string "\"123\n456\"") => [:multi-line "123" "456"])

(tabular
  (fact "about parsing seqs"
    (let [[k & rst] (p/parse-string ?s)]
      k => ?t
      (count (filter (comp #{:whitespace} first) rst)) => ?w
      (count (filter (comp #{:token} first) rst)) => ?c))
  ?s                 ?t           ?w ?c
  "(1 2 3)"          :list        2  3
  "()"               :list        0  0
  "( )"              :list        1  0
  "() "              :list        0  0
  "[1 2 3]"          :vector      2  3
  "[]"               :vector      0  0
  "[ ]"              :vector      1  0
  "[] "              :vector      0  0
  "#{1 2 3}"         :set         2  3
  "#{}"              :set         0  0
  "#{ }"             :set         1  0
  "#{} "             :set         0  0
  "{:a 0 :b 1}"      :map         3  4
  "{}"               :map         0  0
  "{ }"              :map         1  0 
  "{} "              :map         0  0)

(tabular
  (fact "about parsing metadata"
    (let [[k & rst] (p/parse-string (str ?s " s"))]
      k => ?t
      (count (filter (comp not #{:whitespace} first) rst)) => 2
      (last rst) => [:token 's]))
  ?s                   ?t
  "^:private"          :meta
  "^{:private true}"   :meta
  "#^:private"         :meta*
  "#^{:private true}"  :meta*)

(tabular
  (fact "about parsing reader macros"
    (let [[k & rst] (p/parse-string ?s)]
      k => ?t
      (count rst) => ?c
      (first (first rst)) => ?tt))
  ?s                         ?t               ?tt           ?c
  "#'a"                      :var             :token        1
  "#(+ % 1)"                 :fn              :token        5
  "#=(+ 1 2)"                :eval            :list         1
  "#macro 1"                 :reader-macro    :token        3)

(tabular 
  (fact "about parsing exceptions"
    (p/parse-string ?s) => (throws Exception ?p))
  ?s                      ?p
  "#"                     #".*expects a value.*"
  "#^"                    #".*expects a value.*"
  "#="                    #".*expects a value.*"
  "#("                    #".*Unexpected EOF.*"
  "(def"                  #".*Unexpected EOF.*"
  "[def"                  #".*Unexpected EOF.*"
  "#{def"                 #".*Unexpected EOF.*"
  "{:a 0"                 #".*Unexpected EOF.*"
  "\"abc"                 #".*EOF.*"
  "#\"abc"                #".*Unexpected EOF.*"
  "(def x 0]"             #".*Unmatched delimiter.*"
  "^:private"             #".*expects two values.*"
  "#^:private"            #".*expects two values.*"
  "#macro"                #".*expects two values.*")

(fact "about parsing multiple forms"
  (p/parse-string-all "1 2 3") 
      => [:forms 
          [:token 1] [:whitespace " "] 
          [:token 2] [:whitespace " "] 
          [:token 3]]
  (p/parse-string-all ";; Hi!\n(def pi 3.14)")
      => [:forms
          [:comment ";; Hi!"]
          [:list
           [:token 'def] [:whitespace " "]
           [:token 'pi] [:whitespace " "]
           [:token 3.14]]])
