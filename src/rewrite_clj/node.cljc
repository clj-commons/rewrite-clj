(ns
  ^{:added "0.4.0"}
  rewrite-clj.node
  (:require [rewrite-clj.node
             coerce
             comment
             fn
             forms
             integer
             keyword
             meta
             protocols
             quote
             reader-macro
             regex
             seq
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

  [rewrite-clj.node.seq
   list-node
   map-node
   namespaced-map-node
   set-node
   vector-node]

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
   linebreak?
   newlines
   newline-node
   spaces
   whitespace-node
   whitespace?
   comma-node
   comma?
   whitespace-nodes])

;; ## Predicates

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))

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
