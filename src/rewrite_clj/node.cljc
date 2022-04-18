;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/node.cljc
(ns rewrite-clj.node
  "Create, update, convert and integorate nodes.

  All nodes represent Clojure/ClojureScript/EDN.

  Because this API contains many functions, we offer the following categorized listing:

  **Node creation**
  [[comma-node]]
  [[comment-node]]
  [[deref-node]]
  [[eval-node]]
  [[fn-node]]
  [[forms-node]]
  [[integer-node]]
  [[keyword-node]]
  [[list-node]]
  [[map-node]]
  [[map-qualifier-node]]
  [[meta-node]]
  [[namespaced-map-node]]
  [[newline-node]]
  [[quote-node]]
  [[raw-meta-node]]
  [[reader-macro-node]]
  [[regex-node]]
  [[set-node]]
  [[string-node]]
  [[syntax-quote-node]]
  [[token-node]]
  [[uneval-node]]
  [[unquote-node]]
  [[unquote-splicing-node]]
  [[var-node]]
  [[vector-node]]
  [[whitespace-node]]

  **Whitespace creation convenience**
  [[spaces]]
  [[newlines]]
  [[comma-separated]]
  [[line-separated]]
  [[whitespace-nodes]]

  **Convert form to node**
  [[coerce]]

  **Convert node to form**
  [[sexpr-able?]]
  [[sexpr]]
  [[sexprs]]
  [[child-sexprs]]

  **Convert node to string**
  [[string]]

  **Node interogation**
  [[tag]]
  [[inner?]]
  [[children]]
  [[length]]
  [[leader-length]]
  [[printable-only?]]

  **Update node**
  [[replace-children]]

  **Namespaced map element support**
  [[map-context-apply]]
  [[map-context-clear]]

  **Test type**
  [[node?]]
  [[comment?]]
  [[whitespace-or-comment?]]
  [[keyword-node?]]
  [[symbol-node?]]
  [[linebreak?]]
  [[comma?]]"
  ^{:added "0.4.0"}
  (:refer-clojure :exclude [string coerce])
  (:require [rewrite-clj.node.coercer]
            [rewrite-clj.node.comment]
            [rewrite-clj.node.extras]
            [rewrite-clj.node.fn]
            [rewrite-clj.node.forms]
            [rewrite-clj.node.integer]
            [rewrite-clj.node.keyword]
            [rewrite-clj.node.meta]
            [rewrite-clj.node.namespaced-map]
            [rewrite-clj.node.protocols]
            [rewrite-clj.node.quote]
            [rewrite-clj.node.reader-macro]
            [rewrite-clj.node.regex]
            [rewrite-clj.node.seq]
            [rewrite-clj.node.stringz]
            [rewrite-clj.node.token]
            [rewrite-clj.node.uneval]
            [rewrite-clj.node.whitespace]))

#?(:clj (set! *warn-on-reflection* true))

;; ## API Facade


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn coerce
  "Coerce `form` to node."
  [form] (rewrite-clj.node.protocols/coerce form))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn children
  "Returns child nodes for `node`."
  [node] (rewrite-clj.node.protocols/children node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn child-sexprs
  "Returns children for `node` converted to Clojure forms.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (rewrite-clj.node.protocols/child-sexprs node))
  ([node opts] (rewrite-clj.node.protocols/child-sexprs node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn inner?
  "Returns true if `node` can have children."
  [node] (rewrite-clj.node.protocols/inner? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn leader-length
  "Returns number of characters before children for `node`."
  [node] (rewrite-clj.node.protocols/leader-length node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn length
  "Return number of characters for the string version of `node`."
  [node] (rewrite-clj.node.protocols/length node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn node?
  "Returns true if `x` is a rewrite-clj created node."
  [x] (rewrite-clj.node.protocols/node? x))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn printable-only?
  "Return true if `node` cannot be converted to an s-expression element."
  [node] (rewrite-clj.node.protocols/printable-only? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn replace-children
  "Returns `node` replacing current children with `children`."
  [node children] (rewrite-clj.node.protocols/replace-children node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn sexpr
  "Return `node` converted to form.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([node] (rewrite-clj.node.protocols/sexpr node))
  ([node opts] (rewrite-clj.node.protocols/sexpr node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn sexpr-able?
  "Return true if [[sexpr]] is supported for `node`'s element type.

   See [related docs in user guide](/doc/01-user-guide.adoc#not-all-clojure-is-sexpr-able)"
  [node] (rewrite-clj.node.protocols/sexpr-able? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn sexprs
  "Return forms for `nodes`. Nodes that do not represent s-expression are skipped.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([nodes] (rewrite-clj.node.protocols/sexprs nodes))
  ([nodes opts] (rewrite-clj.node.protocols/sexprs nodes opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn map-context-apply
  "Applies `map-qualifier` context to `node`"
  [node map-qualifier] (rewrite-clj.node.protocols/map-context-apply node map-qualifier))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn map-context-clear
  "Removes map-qualifier context for `node`"
  [node] (rewrite-clj.node.protocols/map-context-clear node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn string
  "Return the string version of `node`."
  [node] (rewrite-clj.node.protocols/string node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn tag
  "Returns keyword representing type of `node`."
  [node] (rewrite-clj.node.protocols/tag node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node] (rewrite-clj.node.protocols/value node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.comment
(defn comment-node
  "Create node representing a comment with text `s`.

   You may optionally specify a `prefix` of `\";\"` or `\"#!\"`, defaults is `\";\"`.

   Argument `s`:
   - must not include the `prefix`
   - usually includes the trailing newline character, otherwise subsequent nodes will be on the comment line

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/comment-node \"; my comment\\n\")
       n/string)
   ;; => \";; my comment\\n\"

   (-> (n/comment-node \"#!\" \"/usr/bin/env bb\\n\")
       n/string)
   ;; => \"#!/usr/bin/env bb\\n\"
   ```"
  ([s] (rewrite-clj.node.comment/comment-node s))
  ([prefix s] (rewrite-clj.node.comment/comment-node prefix s)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.comment
(defn comment?
  "Returns true if `node` is a comment."
  [node] (rewrite-clj.node.comment/comment? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.extras
(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node] (rewrite-clj.node.extras/whitespace-or-comment? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.fn
(defn fn-node
  "Create node representing an anonymous function with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/fn-node [(n/token-node '+)
                   (n/spaces 1)
                   (n/token-node 1)
                   (n/spaces 1)
                   (n/token-node '%1)])
       n/string)
   ;; => \"#(+ 1 %1)\"
   ```"
  [children] (rewrite-clj.node.fn/fn-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.forms
(defn forms-node
  "Create top-level node wrapping multiple `children`.
   The forms node is equivalent to an implicit `do` at the top-level.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/forms-node [(n/token-node 1)
                      (n/spaces 1)
                      (n/token-node 2)])
       n/string)
   ;; => \"1 2\"
   ```
   "
  [children] (rewrite-clj.node.forms/forms-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.integer
(defn integer-node
  "Create node representing an integer `value` in `base`.

  `base` defaults to 10.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/integer-node 42)
       n/string)
   ;; => \"42\"

   (-> (n/integer-node 31 2)
       n/string)
   ;; => \"2r11111\"
   ```

   Note: the parser does not currently parse to integer-nodes, but they fully supported for output."
  ([value] (rewrite-clj.node.integer/integer-node value))
  ([value base] (rewrite-clj.node.integer/integer-node value base)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.keyword
(defn keyword-node
  "Create a node representing a keyword `k`.

   Optionally include `auto-resolved?`, which defaults to `false`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   ;; unqualified keyword
   (-> (n/keyword-node :kw)
       n/string)
   ;; => \":kw\"

   ;; qualified keyword
   (-> (n/keyword-node :my-prefix/kw)
       n/string)
   ;; => \":my-prefix/kw\"

   ;; keyword auto-resolved to current ns
   (-> (n/keyword-node :kw true)
       n/string)
   ;; => \"::kw\"

   ;; keyword auto-resolved to a namespace with given alias
   (-> (n/keyword-node :ns-alias/kw true)
       n/string)
   ;; => \"::ns-alias/kw\"
   ```"
  ([k auto-resolved?] (rewrite-clj.node.keyword/keyword-node k auto-resolved?))
  ([k] (rewrite-clj.node.keyword/keyword-node k)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.keyword
(defn keyword-node?
  "Returns true if `n` is a node representing a keyword."
  [n] (rewrite-clj.node.keyword/keyword-node? n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.meta
(defn meta-node
  "Create a node representing a form with metadata.

   When creating manually, you can specify `metadata` and `data` and spacing between the 2 elems will be included:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/meta-node (n/keyword-node :foo)
                    (n/vector-node [(n/token-node 1)]))
       n/string)
   ;; => \"^:foo [1]\"

   (-> (n/meta-node (n/map-node [:foo (n/spaces 1) 42])
                    (n/vector-node [(n/token-node 1)]))
       n/string)
   ;; => \"^{:foo 42} [1]\"
   ```
   When specifying a sequence of `children`, spacing is explicit:

   ```Clojure
   (-> (n/meta-node [(n/keyword-node :foo)
                     (n/spaces 1)
                     (n/vector-node [(n/token-node 1)])])
       n/string)
   ;; => \"^:foo [1]\"
   ```
   See also: [[raw-meta-node]]"
  ([children] (rewrite-clj.node.meta/meta-node children))
  ([metadata data] (rewrite-clj.node.meta/meta-node metadata data)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.meta
(defn raw-meta-node
  "Create a node representing a form with metadata that renders to the reader syntax.

   When creating manually, you can specify `metadata` and `data` and spacing between the 2 elems will be included:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/raw-meta-node (n/keyword-node :foo)
                        (n/vector-node [(n/token-node 2)]))
        n/string)
   ;; => \"#^:foo [2]\"

   (-> (n/raw-meta-node (n/map-node [:foo (n/spaces 1) 42])
                        (n/vector-node [(n/token-node 2)]))
       n/string)
   ;; => \"#^{:foo 42} [2]\"
   ```
   When specifying a sequence of `children`, spacing is explicit:

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/raw-meta-node [(n/keyword-node :foo)
                         (n/spaces 1)
                         (n/vector-node [(n/token-node 2)])])
       n/string)
   ;; => \"#^:foo [2]\"
   ```
   See also: [[meta-node]]"
  ([children] (rewrite-clj.node.meta/raw-meta-node children))
  ([metadata data] (rewrite-clj.node.meta/raw-meta-node metadata data)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.namespaced-map
(defn map-qualifier-node
  "Create a map qualifier node.
   The map qualifier node is a child node of [[namespaced-map-node]].

   ```Clojure
   (require '[rewrite-clj.node :as n])

   ;; qualified
   (-> (n/map-qualifier-node false \"my-prefix\")
       n/string)
   ;; => \":my-prefix\"

   ;; auto-resolved to current ns
   (-> (n/map-qualifier-node true nil)
       n/string)
   ;; => \"::\"

   ;; auto-resolve to namespace with alias
   (-> (n/map-qualifier-node true \"my-ns-alias\")
       n/string)
   ;; => \"::my-ns-alias\"
   ```"
  [auto-resolved? prefix] (rewrite-clj.node.namespaced-map/map-qualifier-node auto-resolved? prefix))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.namespaced-map
(defn namespaced-map-node
  "Create a namespaced map node with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/namespaced-map-node [(n/map-qualifier-node true \"my-ns-alias\")
                               (n/spaces 1)
                               (n/map-node [(n/keyword-node :a)
                                            (n/spaces 1)
                                            (n/token-node 1)])])
       n/string)
   ;; => \"#::my-ns-alias {:a 1}\"
   ```

   Map qualifier context is automatically applied to map keys for sexpr support.

   See also [[map-qualifier-node]] and [[map-node]]."
  [children] (rewrite-clj.node.namespaced-map/namespaced-map-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.regex
(defn regex-node
  "Create node representing a regex with `pattern-string`.
   Use same escape rules for `pattern-string` as you would for `(re-pattern \"pattern-string\")`

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/regex-node \"my\\\\.lil.*regex\")
       n/string)
   ;; => \"#\\\"my\\\\.lil.*regex\\\"\"
   ```"
  [pattern-string] (rewrite-clj.node.regex/regex-node pattern-string))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn deref-node
  "Create node representing the dereferencing of a form
   where `children` is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/deref-node (n/token-node 'my-var))
       n/string)
   ;; => \"@my-var\"

   ;; specifying a sequence allows for whitespace between @ and form
   (-> (n/deref-node [(n/spaces 2)
                      (n/token-node 'my-var)])
       n/string)
   ;; => \"@  my-var\"
   ```"
  [children] (rewrite-clj.node.reader-macro/deref-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn eval-node
  "Create node representing an inline evaluation
   where `children` is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/eval-node (n/list-node [(n/token-node 'inc)
                                  (n/spaces 1)
                                  (n/token-node 1)]))
       n/string)
   ;; => \"#=(inc 1)\"

   ;; specifying a sequence allows for whitespace between the
   ;; prefix and the form
   (-> (n/eval-node [(n/spaces 3)
                     (n/list-node [(n/token-node 'inc)
                                   (n/spaces 1)
                                   (n/token-node 1)])])
       n/string)
   ;; => \"#=   (inc 1)\"
   ```"
  [children] (rewrite-clj.node.reader-macro/eval-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn reader-macro-node
  "Create node representing a reader macro with `macro-node` and `form-node` or `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   ;; here we call with macro-node and form-node
   (-> (n/reader-macro-node (n/token-node 'my-macro)
                            (n/token-node 42))
       n/string)
   ;; => \"#my-macro 42\"

   ;; calling with a sequence of children gives us control over whitespace
   (-> (n/reader-macro-node [(n/token-node 'my-macro)
                             (n/spaces 4)
                             (n/token-node 42)])
       n/string)
   ;; => \"#my-macro    42\"
   ```"
  ([children] (rewrite-clj.node.reader-macro/reader-macro-node children))
  ([macro-node form-node] (rewrite-clj.node.reader-macro/reader-macro-node macro-node form-node)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn var-node
  "Create node representing a var where `children` is either a
   sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/var-node (n/token-node 'my-var))
       n/string)
   ;; => \"#'my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; prefix and the var
   (-> (n/var-node [(n/spaces 2)
                    (n/token-node 'my-var)])
       n/string)
   ;; => \"#'  my-var\"
   ```"
  [children] (rewrite-clj.node.reader-macro/var-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn list-node
  "Create a node representing a list with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/list-node [(n/token-node 1)
                     (n/spaces 1)
                     (n/token-node 2)
                     (n/spaces 1)
                     (n/token-node 3)])
       n/string)
   ;; => \"(1 2 3)\"
   ```"
  [children] (rewrite-clj.node.seq/list-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn map-node
  "Create a node representing a map with `children`.
   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/map-node [(n/keyword-node :a)
                    (n/spaces 1)
                    (n/token-node 1)
                    (n/spaces 1)
                    (n/keyword-node :b)
                    (n/spaces 1)
                    (n/token-node 2)])
       (n/string))
   ;; => \"{:a 1 :b 2}\"
   ```

   Note that rewrite-clj allows the, technically illegal, unbalanced map:
   ```Clojure
   (-> (n/map-node [(n/keyword-node :a)])
       (n/string))
   ;; => \"{:a}\"
   ```
   See [docs on unbalanced maps](/doc/01-user-guide.adoc#unbalanced-maps).

   Rewrite-clj also allows the, also technically illegal, map with duplicate keys:
   ```Clojure
   (-> (n/map-node [(n/keyword-node :a)
                    (n/spaces 1)
                    (n/token-node 1)
                    (n/spaces 1)
                    (n/keyword-node :a)
                    (n/spaces 1)
                    (n/token-node 2)])
       (n/string))
   ;; => \"{:a 1 :a 2}\"
   ```
   See [docs on maps with duplicate keys](/doc/01-user-guide.adoc#maps-with-duplicate-keys)."
  [children] (rewrite-clj.node.seq/map-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn set-node
  "Create a node representing a set with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/set-node [(n/token-node 1)
                    (n/spaces 1)
                    (n/token-node 2)
                    (n/spaces 1)
                    (n/token-node 3)])
       n/string)
   ;; => \"#{1 2 3}\"
   ```

   Note that rewrite-clj allows the, technically illegal, set with duplicate values:
   ```Clojure
   (-> (n/set-node [(n/token-node 1)
                    (n/spaces 1)
                    (n/token-node 1)])
       (n/string))
   ;; => \"#{1 1}\"
   ```

   See [docs on sets with duplicate values](/doc/01-user-guide.adoc#sets-with-duplicate-values)."
  [children] (rewrite-clj.node.seq/set-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn vector-node
  "Create a node representing a vector with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/vector-node [(n/token-node 1)
                       (n/spaces 1)
                       (n/token-node 2)
                       (n/spaces 1)
                       (n/token-node 3)])
       n/string)
   ;; => \"[1 2 3]\"
   ```"
  [children] (rewrite-clj.node.seq/vector-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.stringz
(defn string-node
  "Create node representing a string value where `lines` can be a sequence of strings or a single string.

  When `lines` is a sequence, the resulting node will `tag` will be `:multi-line`, otherwise `:token`.

  ```Clojure
  (require '[rewrite-clj.node :as n])

  (-> (n/string-node \"hello\")
      n/string)
  ;; => \"\\\"hello\\\"\"

  (-> (n/string-node [\"line1\" \"\" \"line3\"])
       n/string)
  ;; => \"\\\"line1\\n\\nline3\\\"\"
  ```"
  [lines] (rewrite-clj.node.stringz/string-node lines))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn quote-node
  "Create node representing a single quoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/quote-node (n/token-node 'sym))
       (n/string))
   ;; => \"'sym\"

   ;; specifying a sequence allows for whitespace between the
   ;; quote and the quoted
   (-> (n/quote-node [(n/spaces 10)
                      (n/token-node 'sym1) ])
       n/string)
   ;; => \"'          sym1\"
   ```"
  [children] (rewrite-clj.node.quote/quote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn syntax-quote-node
  "Create node representing a single syntax-quoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/syntax-quote-node (n/token-node 'map))
       n/string)
   ;; => \"`map\"

   ;; specifying a sequence allows for whitespace between the
   ;; syntax quote and the syntax quoted
   (-> (n/syntax-quote-node [(n/spaces 3)
                             (n/token-node 'map)])
       n/string)
   ;; => \"`   map\"
   ```"
  [children] (rewrite-clj.node.quote/syntax-quote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn unquote-node
  "Create node representing a single unquoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/unquote-node (n/token-node 'my-var))
       n/string)
   ;; => \"~my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; unquote and the uquoted
   (-> (n/unquote-node [(n/spaces 4)
                        (n/token-node 'my-var)])
       n/string)
   ;; => \"~    my-var\"
   ```"
  [children] (rewrite-clj.node.quote/unquote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn unquote-splicing-node
  "Create node representing a single unquote-spliced form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/unquote-splicing-node (n/token-node 'my-var))
       n/string)
   ;; => \"~@my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; splicing unquote and the splicing unquoted
   (-> (n/unquote-splicing-node [(n/spaces 2)
                                 (n/token-node 'my-var)])
       n/string)
   ;; => \"~@  my-var\"
   ```"
  [children] (rewrite-clj.node.quote/unquote-splicing-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.token
(defn token-node
  "Create node for an unspecified token of `value`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/token-node 'sym) n/string)
   ;; => \"sym\"

   (-> (n/token-node 42) n/string)
   ;; => \"42\"
   ```"
  ([value] (rewrite-clj.node.token/token-node value))
  ([value string-value] (rewrite-clj.node.token/token-node value string-value)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.token
(defn symbol-node?
  "Returns true if `n` is a node representing a symbol."
  [n] (rewrite-clj.node.token/symbol-node? n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.uneval
(defn uneval-node
  "Create node representing an unevaled form with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/uneval-node [(n/spaces 1)
                       (n/token-node 42)])
       n/string)
   ;; => \"#_ 42\"
   ```"
  [children] (rewrite-clj.node.uneval/uneval-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn comma-separated
  "Interleave `nodes` with `\", \"` nodes."
  [nodes] (rewrite-clj.node.whitespace/comma-separated nodes))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn line-separated
  "Interleave `nodes` with newline nodes."
  [nodes] (rewrite-clj.node.whitespace/line-separated nodes))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn linebreak?
  "Returns true if `node` represents one or more linebreaks."
  [node] (rewrite-clj.node.whitespace/linebreak? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn newlines
  "Create node representing `n` newline characters."
  [n] (rewrite-clj.node.whitespace/newlines n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn newline-node
  "Create newline node of string `s`, where `s` is one or more linebreak characters."
  [s] (rewrite-clj.node.whitespace/newline-node s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn spaces
  "Create node representing `n` spaces."
  [n] (rewrite-clj.node.whitespace/spaces n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn whitespace-node
  "Create whitespace node of string `s`, where `s` is one or more space characters."
  [s] (rewrite-clj.node.whitespace/whitespace-node s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn whitespace?
  "Returns true if `node` represents Clojure whitespace."
  [node] (rewrite-clj.node.whitespace/whitespace? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn comma-node
  "Create comma node of string `s`, where `s` is one or more comma characters."
  [s] (rewrite-clj.node.whitespace/comma-node s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn comma?
  "Returns true if `node` represents one or more commas."
  [node] (rewrite-clj.node.whitespace/comma? node))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.whitespace
(defn whitespace-nodes
  "Convert string `s` of whitespace to whitespace/newline nodes."
  [s] (rewrite-clj.node.whitespace/whitespace-nodes s))
