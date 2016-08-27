(ns
  ^{:added "0.4.0"}
  rewrite-clj.node
  (:refer-clojure :exclude [seq? list? vector? set? map? keyword? string?
                            integer? fn?])
  (:require [rewrite-clj.node
             coerce
             comment
             fn
             forms
             integer
             keyword
             meta
             predicates
             protocols
             quote
             reader-macro
             regex
             seqs
             string
             token
             uneval
             whitespace]
            [rewrite-clj.potemkin :refer [import-vars]]))

;; ## API Facade

(import-vars
  [rewrite-clj.node.protocols
   coerce
   children
   child-sexprs
   concat-strings
   inner?
   leader-length
   length
   printable-only?
   replace-children
   sexpr
   sexprs
   string
   tag]

  [rewrite-clj.node.comment
   comment-node
   comment?]

  [rewrite-clj.node.fn
   fn-node]

  [rewrite-clj.node.forms
   forms-node]

  [rewrite-clj.node.integer
   integer-node]

  [rewrite-clj.node.keyword
   keyword-node]

  [rewrite-clj.node.meta
   meta-node
   raw-meta-node]

  [rewrite-clj.node.regex
   regex-node]

  [rewrite-clj.node.reader-macro
   deref-node
   eval-node
   reader-macro-node
   var-node]

  [rewrite-clj.node.seqs
   vector-node
   list-node
   set-node
   map-node]

  [rewrite-clj.node.string
   string-node]

  [rewrite-clj.node.quote
   quote-node
   syntax-quote-node
   unquote-node
   unquote-splicing-node]

  [rewrite-clj.node.token
   token-node]

  [rewrite-clj.node.uneval
   uneval-node]

  [rewrite-clj.node.whitespace
   comma-separated
   line-separated
   newlines
   newline-node
   spaces
   whitespace-node
   comma-node
   whitespace-nodes]

  [rewrite-clj.node.predicates
   node?
   seq?
   list?
   vector?
   set?
   map?
   quote?
   uneval?
   token?
   integer?
   keyword?
   string?
   comment?
   whitespace?
   linebreak? newline?
   comma?
   whitespace-or-comment?
   fn?
   meta?
   deref?
   forms?
   form?
   reader-macro?
   reader?])

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
            (first)
            ((juxt tag sexpr)))
    (sexpr node)))
