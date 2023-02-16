(ns ^:no-doc rewrite-clj.node.stringz
  (:require [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [rewrite-clj.node.protocols :as node] ))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- wrap-string [s]
  (str "\"" s "\""))

(defn- join-lines [lines]
  (string/join "\n" lines))

(defrecord StringNode [lines]
  node/Node
  (tag [_node]
    (if (next lines)
      :multi-line
      :token))
  (node-type [_node] :string)
  (printable-only? [_node] false)
  (sexpr* [_node _opts]
    (join-lines
      (map
        (comp edn/read-string wrap-string)
        lines)))
  (length [_node]
    (+ 2 (reduce + (map count lines))))
  (string [_node]
    (wrap-string (join-lines lines)))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! StringNode)

;; ## Constructors

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
  - but the string should not wrapped with `\\\"`

  Here's an example of conforming to these expectations.
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
  [lines]
  (if (string? lines)
    (->StringNode [lines])
    (->StringNode lines)))
