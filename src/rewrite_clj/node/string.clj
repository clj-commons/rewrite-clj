;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/node/string.clj
(ns ^:no-doc rewrite-clj.node.string
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:require [rewrite-clj.node.stringz]))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.stringz
(defn string-node
  "Create node representing a string value where `lines` can be a sequence of strings or a single string.

  When `lines` is a sequence, the resulting node `tag` will be `:multi-line`, otherwise `:token`.

  `:multi-line` refers to a single string in your source that appears over multiple lines, for example:

  ```Clojure
  (def s \"foo
            bar
              baz\")
  ```

  It does not apply to a string that appears on a single line that includes escaped newlines, for example:

  ```Clojure
  (def s \"foo\\nbar\\n\\baz\")
  ```

  Naive examples (see example on escaping below):

  ```Clojure
  (require '[rewrite-clj.node :as n])

  (-> (n/string-node \"hello\")
      n/string)
  ;; => \"\\\"hello\\\"\"

  (-> (n/string-node [\"line1\" \"\" \"line3\"])
       n/string)
  ;; => \"\\\"line1\\n\\nline3\\\"\"
  ```

  This function was originally written to serve the rewrite-clj parser.
  Escaping and wrapping expectations are non-obvious.
  - characters within strings are assumed to be escaped
  - the string should not wrapped with `\\\"`

  (Best to view this on cljdoc, docstring string escaping is confusing).

  ```Clojure
  (require '[clojure.string :as string])

  (defn pr-str-unwrapped [s]
    (apply str (-> s pr-str next butlast)))

  (-> \"hey \\\" man\"
      pr-str-unwrapped
      n/string-node
      n/string)
  ;; => \"\\\"hey \\\\\\\" man\\\"\"
  ```

  To construct strings appearing on a single line, consider [[token-node]].
  It will handle escaping for you."
  [lines] (rewrite-clj.node.stringz/string-node lines))
