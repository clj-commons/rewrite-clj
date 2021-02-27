;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/node.cljc
(ns rewrite-clj.node
  "Create and evaluate nodes.

  All nodes represent Clojure/ClojureScript/EDN."
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
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (rewrite-clj.node.protocols/sexpr node))
  ([node opts] (rewrite-clj.node.protocols/sexpr node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.protocols
(defn sexprs
  "Return forms for `nodes`. Nodes that do not represent s-expression are skipped.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
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
  "Create node representing a comment with text `s`."
  [s] (rewrite-clj.node.comment/comment-node s))

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
  "Create node representing an anonymous function with `children`."
  [children] (rewrite-clj.node.fn/fn-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.forms
(defn forms-node
  "Create top-level node wrapping multiple `children`
   (equivalent to an implicit `do` at the top-level)."
  [children] (rewrite-clj.node.forms/forms-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.integer
(defn integer-node
  "Create node representing an integer `value` in `base`.

  `base` defaults to 10.

  Note: the parser does not currently parse to integer-nodes, but the write can handle them just fine."
  ([value] (rewrite-clj.node.integer/integer-node value))
  ([value base] (rewrite-clj.node.integer/integer-node value base)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.keyword
(defn keyword-node
  "Create a node representing a keyword `k`.

  Examples usages:
  * `(keyword-node :kw)` - unqualified
  * `(keyword-node :my-prefix/kw)` - qualified

  You can pass `true` for the optional `auto-resolved?` parameter:
  * `(keyword-node :kw true)` - auto-resolved to current ns, equivalent to code `::kw`
  * `(keyword-node :ns-alias/kw true)` - auto-resolved to namespace with alias ns-alias, equivalent to code `::ns-alias/kw`"
  ([k auto-resolved?] (rewrite-clj.node.keyword/keyword-node k auto-resolved?))
  ([k] (rewrite-clj.node.keyword/keyword-node k)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.keyword
(defn keyword-node?
  "Returns true if `n` is a node representing a keyword."
  [n] (rewrite-clj.node.keyword/keyword-node? n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.meta
(defn meta-node
  "Create node representing a form `data` and its `metadata`."
  ([children] (rewrite-clj.node.meta/meta-node children))
  ([metadata data] (rewrite-clj.node.meta/meta-node metadata data)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.meta
(defn raw-meta-node
  "Create node representing a form `data` and its `metadata` using the
   `#^` prefix."
  ([children] (rewrite-clj.node.meta/raw-meta-node children))
  ([metadata data] (rewrite-clj.node.meta/raw-meta-node metadata data)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.namespaced-map
(defn map-qualifier-node
  "Create a map qualifier node.

  - `(map-qualifier-node false \"my-prefix\")` -> `#:my-prefix` - qualified
  - `(map-qualifier-node true \"my-ns-alias\")` -> `#::my-ns-alias` - auto-resolved namespace alias
  - `(map-qualifier-node true nil)` -> `#::` - auto-resolved current namespace

  The above are the only supported variations, use [[rewrite-clj.node/map-node]] for unqualified maps."
  [auto-resolved? prefix] (rewrite-clj.node.namespaced-map/map-qualifier-node auto-resolved? prefix))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.namespaced-map
(defn namespaced-map-node
  "Create a namespaced map node with `children`.

  - first child must be a map-qualifier node, see [[rewrite-clj.node/map-qualifier-node]]
  - optionally followed by whitespace node(s),
  - followed by a map node, see [[rewrite-clj.node/map-node]]"
  [children] (rewrite-clj.node.namespaced-map/namespaced-map-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.regex
(defn regex-node
  "Create node representing a regex with `pattern-string`.
   Use same escape rules for `pattern-string` as you would for `(re-pattern \"pattern-string\")`"
  [pattern-string] (rewrite-clj.node.regex/regex-node pattern-string))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn deref-node
  "Create node representing the dereferencing of a form (i.e. `@...`)
   where `children` is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.reader-macro/deref-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn eval-node
  "Create node representing an inline evaluation (i.e. `#=...`)
   where `children` is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.reader-macro/eval-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn reader-macro-node
  "Create node representing a reader macro (i.e. `#... ...`) with `children`. "
  ([children] (rewrite-clj.node.reader-macro/reader-macro-node children))
  ([macro-node form-node] (rewrite-clj.node.reader-macro/reader-macro-node macro-node form-node)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.reader-macro
(defn var-node
  "Create node representing a var
   where `children` is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.reader-macro/var-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn list-node
  "Create a node representing a list with `children`."
  [children] (rewrite-clj.node.seq/list-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn map-node
  "Create a node representing an map with `children`."
  [children] (rewrite-clj.node.seq/map-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn set-node
  "Create a node representing a set with `children`."
  [children] (rewrite-clj.node.seq/set-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.seq
(defn vector-node
  "Create a node representing a vector with `children`."
  [children] (rewrite-clj.node.seq/vector-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.stringz
(defn string-node
  "Create node representing a string value where `lines`
   can be a sequence of strings or a single string."
  [lines] (rewrite-clj.node.stringz/string-node lines))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn quote-node
  "Create node representing a quoted form where `children`
   is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.quote/quote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn syntax-quote-node
  "Create node representing a syntax-quoted form where `children`
   is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.quote/syntax-quote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn unquote-node
  "Create node representing an unquoted form (i.e. `~...`) where `children`.
   is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.quote/unquote-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.quote
(defn unquote-splicing-node
  "Create node representing an unquote-spliced form (i.e. `~@...`) where `children`.
   is either a sequence of nodes or a single node."
  [children] (rewrite-clj.node.quote/unquote-splicing-node children))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.token
(defn token-node
  "Create node for an unspecified token of `value`."
  ([value] (rewrite-clj.node.token/token-node value))
  ([value string-value] (rewrite-clj.node.token/token-node value string-value)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.token
(defn symbol-node?
  "Returns true if `n` is a node representing a symbol."
  [n] (rewrite-clj.node.token/symbol-node? n))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.uneval
(defn uneval-node
  "Create node representing an uneval `#_` form with `children`."
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
