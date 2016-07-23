(ns
  ^{:added "0.4.0"}
  rewrite-clj.node
  (:refer-clojure :exclude [seq? list? vector? set? map? keyword? string?])
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
<<<<<<< 7bc97765ac781d925c41c0ca6d87c09b07ef866f
<<<<<<< b20062b2d35539f767e682f043408def8ed47c82

  [rewrite-clj.node.vector vector-node]
  [rewrite-clj.node.list list-node]
  [rewrite-clj.node.set set-node]
  [rewrite-clj.node.map map-node]
=======

  [rewrite-clj.node.colls vector-node list-node set-node map-node]
>>>>>>> Everything in node/coll.clj, no more traits
=======

  [rewrite-clj.node.seqs
   vector-node
   list-node
   set-node
   map-node]
>>>>>>> Adds a bunch of predicates

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
   keyword?
   string?
   comment?
   whitespace?
   linebreak?
   comma?
   whitespace-or-comment?
   form?])

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
