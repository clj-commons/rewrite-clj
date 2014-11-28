(ns rewrite-clj.node
  (:require [rewrite-clj.node
             comment
             forms
             integer
             keyword
             meta
             protocols
             quote
             reader-macro
             seq
             string
             token
             uneval
             whitespace]
            [potemkin :refer [import-vars]]))

;; ## API Facade

(import-vars
  [rewrite-clj.node.protocols
   coerce
   children
   concat-strings
   inner?
   printable-only?
   replace-children
   sexpr
   sexprs
   string
   tag]

  [rewrite-clj.node.comment
   comment-node
   comment?]

  [rewrite-clj.node.forms
   forms-node]

  [rewrite-clj.node.integer
   integer-node]

  [rewrite-clj.node.keyword
   keyword-node]

  [rewrite-clj.node.meta
   meta-node
   raw-meta-node]

  [rewrite-clj.node.reader-macro
   deref-node
   eval-node
   fn-node
   reader-macro-node
   uneval-node
   var-node]

  [rewrite-clj.node.seq
   list-node
   map-node
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
   whitespace?])

;; ## Predicates

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))
