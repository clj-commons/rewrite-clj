(ns ^{ :doc "Tests for EDN parser."
       :author "Yannick Scherer" }
  rewrite-clj.parser-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj
             [node :as node]
             [parser :as p]]))

(tabular
  (fact "about parsing the first few whitespaces."
        (let [n (p/parse-string ?ws)]
          (node/tag n) => :whitespace
          (node/string n) => ?parsed))
  ?ws       ?parsed
  "   "     "   "
  "   \n  " "   ")

(tabular
  (fact "about parsing whitespace strings."
        (let [n (p/parse-string-all ?ws)]
          (node/tag n)    => :forms
          (node/string n) => (.replace ?ws "\r\n" "\n")
          (map (juxt node/tag node/string) (node/children n)) => ?children))
  ?ws            ?children
  "   \n   "     [[:whitespace "   "]
                  [:newline "\n"]
                  [:whitespace "   "]]
  "\u2028"       [[:whitespace "\u2028"]]
  " \t \r\n \t " [[:whitespace " \t "]
                  [:newline "\n"]
                  [:whitespace " \t "]])

(tabular
  (fact "about parsing simple data"
        (let [n (p/parse-string ?s)]
          (node/tag n)    => :token
          (node/string n) => ?s
          (node/sexpr n)  => ?r))
  ?s                           ?r
  "0"                          0
  "0.1"                        0.1
  "12e10"                      1.2e11
  "2r1100"                     12
  "1N"                         1N
  ":key"                       :key
  "\\\\"                       \\
  "\\a"                        \a
  "\\space"                    \space
  "\\'"                        \'
  ":1.5"                       :1.5
  ":1.5.0"                     :1.5.0
  ":ns/key"                    :ns/key
  ":key:key"                   :key:key
  "::1.5.1"                    ::1.5.1
  "::key"                      ::key
  "::xyz/key"                  :xyz/key
  ":x'"                        :x'
  "sym"                        'sym
  "sym#"                       'sym#
  "sym'"                       'sym'
  "sym'sym"                    'sym'sym
  "sym:sym"                    'sym:sym
  "\"string\""                 "string")

(tabular
  (fact "about parsing reader-prefixed data."
        (let [n (p/parse-string ?s)
              children (node/children n)
              c (map node/tag children)]
          (node/tag n) => ?t
          (last c)     => :token
          (node/sexpr n) => ?sexpr
          (node/sexpr (last children)) => 'sym
          (vec (butlast c))  => ?ws))
  ?s                     ?t                ?ws             ?sexpr
  "@sym"                 :deref            []              '(deref sym)
  "@  sym"               :deref            [:whitespace]   '(deref sym)
  "'sym"                 :quote            []              '(quote sym)
  "'  sym"               :quote            [:whitespace]   '(quote sym)
  "`sym"                 :syntax-quote     []              '(quote sym)
  "`  sym"               :syntax-quote     [:whitespace]   '(quote sym)
  "~sym"                 :unquote          []              '(unquote sym)
  "~  sym"               :unquote          [:whitespace]   '(unquote sym)
  "~@sym"                :unquote-splicing []              '(unquote-splicing sym)
  "~@  sym"              :unquote-splicing [:whitespace]   '(unquote-splicing sym)
  "#=sym"                :eval             []              '(eval 'sym)
  "#=  sym"              :eval             [:whitespace]   '(eval 'sym)
  "#'sym"                :var              []              '(var sym)
  "#'\nsym"              :var              [:newline]      '(var sym))

(fact "about eval."
      (let [n (p/parse-string "#=(+ 1 2)")]
        (node/tag n) => :eval
        (node/string n) => "#=(+ 1 2)"
        (node/sexpr n) => '(eval '(+ 1 2))))

(fact "about uneval."
      (let [s "#' #_    (+ 1 2) sym"
            n (p/parse-string s)
            [ws0 uneval ws1 sym] (node/children n)]
        (node/tag n) => :var
        (node/string n) => s
        (node/tag ws0) => :whitespace
        (node/tag ws1) => :whitespace
        (node/tag sym) => :token
        (node/sexpr sym) => 'sym
        (node/tag uneval) => :uneval
        (node/string uneval) => "#_    (+ 1 2)"
        (node/printable-only? uneval) => true
        (node/sexpr uneval) => (throws UnsupportedOperationException)))

(tabular
  (fact "about parsing regular expressions"
    (let [n (p/parse-string ?s)]
      (node/tag n) => :regex
      (node/sexpr n) => ?p))
  ?s                 ?p

  "#\"regex\""       '(re-pattern "regex")
  "#\"regex\\.\""    '(re-pattern "regex\\.")
  "#\"[reg|k].x\""   '(re-pattern "[reg|k].x")
  "#\"a\\nb\""       '(re-pattern "a\\nb")
  "#\"a\nb\""        '(re-pattern "a\nb"))

(tabular
  (fact "about parsing strings"
        (let [n (p/parse-string ?s)]
          (node/tag n) => ?tag
          (node/string n) => ?s
          (node/sexpr n) => ?sexpr))
  ?s              ?tag         ?sexpr
  "\"123\""       :token       "123"
  "\"123\\n456\"" :token       "123\n456"
  "\"123\n456\""  :multi-line  "123\n456")

(tabular
  (fact "about parsing seqs"
    (let [n (p/parse-string ?s)
          children (node/children n)
          fq (frequencies (map node/tag children))]
      (node/tag n)       => ?t
      (node/string n)    => (.trim ?s)
      (node/sexpr n)     =(read-string ?s)
      (:whitespace fq 0) => ?w
      (:token fq 0)      => ?c))
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
    (let [s (str ?s " s")
          n (p/parse-string s)
          [mta ws sym] (node/children n)]
      (node/tag n)          => ?t
      (node/string n)       => s
      (node/sexpr n)        => 's
      (meta (node/sexpr n)) => {:private true}
      (node/tag mta)        => ?mt
      (node/tag ws)         => :whitespace
      (node/tag sym)        => :token
      (node/sexpr sym)      => 's))
  ?s                   ?t     ?mt
  "^:private"          :meta  :token
  "^{:private true}"   :meta  :map
  "#^:private"         :meta* :token
  "#^{:private true}"  :meta* :map)

(tabular
  (fact "about parsing multiple metadata forms"
        (let [s (str ?s " s")
              n (p/parse-string s)
              [mta ws n'] (node/children n)
              [mta2 ws2 sym] (node/children n')]
          ;; outer meta
          (node/tag n)          => ?t
          (node/string n)       => s
          (node/sexpr n)        => 's
          (meta (node/sexpr n)) => {:private true :awe true}
          (node/tag mta)        => ?mt
          (node/tag ws)         => :whitespace

          ;; inner meta
          (node/tag n')          => ?t
          (meta (node/sexpr n')) => {:awe true}
          (node/tag mta2)        => ?mt
          (node/tag ws2)         => :whitespace))
  ?s                                ?t     ?mt
  "^:private ^:awe"                 :meta  :token
  "^{:private true} ^{:awe true}"   :meta  :map
  "#^:private #^:awe"               :meta* :token
  "#^{:private true} #^{:awe true}" :meta* :map)

(tabular
  (fact "about parsing reader macros"
    (let [n (p/parse-string ?s)]
      (node/tag n) => ?t
      (node/string n) => ?s
      (map node/tag (node/children n)) => ?children))
  ?s                         ?t               ?children
  "#'a"                      :var             [:token]
  "#=(+ 1 2)"                :eval            [:list]
  "#macro 1"                 :reader-macro    [:token :whitespace :token]
  "#macro (* 2 3)"           :reader-macro    [:token :whitespace :list]
  "#?(:clj bar)"             :reader-macro    [:token :list]
  "#? (:clj bar)"            :reader-macro    [:token :whitespace :list]
  "#?@ (:clj bar)"           :reader-macro    [:token :whitespace :list]
  "#?foo baz"                :reader-macro    [:token :whitespace :token]
  "#_abc"                    :uneval          [:token]
  "#_(+ 1 2)"                :uneval          [:list]
  "#(+ % 1)"                 :fn              [:token :whitespace
                                               :token :whitespace
                                               :token])

(tabular
  (fact "about parsing comments."
        (let [n (p/parse-string ?s)]
          n => node/printable-only?
          (node/tag n) => :comment
          (node/string n) => ?s))
  ?s
  "; this is a comment\n"
  ";; this is a comment\n"
  "; this is a comment"
  ";; this is a comment"
  ";"
  ";;"
  ";\n"
  ";;\n")

(tabular
  (fact "about parsing exceptions"
    (p/parse-string ?s) => (throws Exception ?p))
  ?s                      ?p
  "#"                     #".*Unexpected EOF.*"
  "#("                    #".*Unexpected EOF.*"
  "(def"                  #".*Unexpected EOF.*"
  "[def"                  #".*Unexpected EOF.*"
  "#{def"                 #".*Unexpected EOF.*"
  "{:a 0"                 #".*Unexpected EOF.*"
  "\"abc"                 #".*EOF.*"
  "#\"abc"                #".*Unexpected EOF.*"
  "(def x 0]"             #".*Unmatched delimiter.*"
  "#="                    #".*:eval node expects 1 value.*"
  "#^"                    #".*:meta node expects 2 values.*"
  "^:private"             #".*:meta node expects 2 values.*"
  "#^:private"            #".*:meta node expects 2 values.*"
  "#_"                    #".*:uneval node expects 1 value.*"
  "#'"                    #".*:var node expects 1 value.*"
  "#macro"                #".*:reader-macro node expects 2 values.*")

(fact "about parsing multiple forms"
  (let [s "1 2 3"
        n (p/parse-string-all s)
        children (node/children n)]
    (node/tag n) => :forms
    (node/string n) => s
    (node/sexpr n) => '(do 1 2 3)
    (map node/tag children) => [:token :whitespace
                                :token :whitespace
                                :token])
  (let [s ";; Hi!\n(def pi 3.14)"
        n (p/parse-string-all s)
        children (node/children n)]
    (node/tag n) => :forms
    (node/string n) => s
    (node/sexpr n) => '(def pi 3.14)
    (map node/tag children) => [:comment :list]
    (node/string (first children))))

(fact "about parsing files"
  (let [f (doto (java.io.File/createTempFile "rewrite.test" "")
            (.deleteOnExit))
        s "âbcdé"
        c ";; Hi"
        o (str c "\n\n" (pr-str s))]
    (spit f o) => anything
    (slurp f) => o
    (let [n (p/parse-file-all f)
          children (node/children n)]
      (node/tag n) => :forms
      (node/string n) => o
      (node/sexpr n) => s
      (map node/tag children) => [:comment :newline :token]
      (map node/string children) => [";; Hi\n" "\n" (pr-str s)])))

(defn- nodes-with-meta
  "Create map associating row/column number pairs with the node at that position."
  [n]
  (let [start-pos ((juxt :row :col) (meta n))
        end-pos ((juxt :end-row :end-col) (meta n))
        entry {start-pos {:node n, :end-pos end-pos}}]
    (if (node/inner? n)
      (->> (node/children n)
           (map nodes-with-meta)
           (into entry))
      entry)))

(let [s "(defn f\n  [x]\n  (println x))"
      positions (->> (p/parse-string-all s)
                     (nodes-with-meta))]
  (tabular
    (fact "about row/column metadata."
          (let [{:keys [node end-pos]} (positions ?pos)]
            (node/tag node)    => ?t
            (node/string node) => ?s
            (node/sexpr node)  => ?sexpr
            #_(fact
                "reliable decision on end pos not currently possible."
                end-pos => ?end)))
    ?pos   ?end   ?t      ?s             ?sexpr
    [1 1]  [3 14] :list   s              '(defn f [x] (println x))
    [1 2]  [1 5]  :token  "defn"         'defn
    [1 7]  [1 8]  :token  "f"            'f
    [2 3]  [2 5]  :vector "[x]"          '[x]
    [2 4]  [2 5]  :token  "x"            'x
    [3 3]  [3 13] :list   "(println x)"  '(println x)
    [3 4]  [3 10] :token  "println"      'println
    [3 12] [3 13] :token  "x"            'x))
