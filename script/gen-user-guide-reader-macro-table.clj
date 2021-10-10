;; run via: clojure -M
;; generates user guide reader macro example table, see user guide for comment
(require '[rewrite-clj.node :as n]
         '[rewrite-clj.parser :as p])

(->> [["Quote" ["'form"]]
      ["Character" ["\\newline" "\\space" "\\tab"]]
      ["Comment" ["; comment"]]
      ["Deref" ["@form"]]
      ["Metadata" ["^{:a 1 :b 2} [1 2 3]"
                   "^String x"
                   "^:dynamic x"]]
      ["Set" ["#{1 2 3}"]]
      ["Regex" ["#\"reg.*ex\""]]
      ["Var-quote" ["#'x"]]
      ["Anonymous function" ["#(println %)"]]
      ["Ignore next form" ["#_ :ignore-me"]]
      ["Syntax quote" ["`symbol"]]
      ["Syntax unquote" ["~symbol"]]
      ["Tagged literal" ["#foo/bar [1 2 3]"
                         "#inst \"2018-03-28T10:48:00.000\""
                         "#uuid \"3b8a31ed-fd89-4f1b-a00f-42e3d60cf5ce\""]]
      ["Reader conditional" ["#?(:clj x :cljs y)"
                             "#@?(:clj [x] :cljs [y])"]]]
     (reduce (fn [sdoc [desc inputs]]
               (let [parsed (map p/parse-string inputs)
                     tag (-> parsed first n/tag)
                     _ (assert (every? #(= (n/tag %) tag) parsed) "oops, expected tags to be same")
                     outputs (map #(try (n/sexpr %) (catch Exception _e nil))
                                  parsed)
                     example-rows (map #(format "a|`%s`\na|%s\n" %1
                                                (if %2
                                                  (str "`" (binding [*print-meta* true]
                                                             (pr-str %2)) "`")
                                                  "<unsupported operation>"))
                                       inputs outputs)]
                    (str sdoc
                         (format  "2+a|*%s* `%s`\n" desc tag)
                         (apply str example-rows)
                         "\n")))
             "")
     (format "|===\n| Parsed input | Node sexpr\n\n%s|===")
     println)
