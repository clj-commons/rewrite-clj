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

#_{:import-vars/import
   {:from [[rewrite-clj.node.protocols
            coerce
            children
            child-sexprs
            inner?
            leader-length
            length
            node?
            printable-only?
            replace-children
            sexpr
            sexpr-able?
            sexprs
            map-context-apply
            map-context-clear
            string
            tag
            ^{:deprecated "0.4.0"} value]

           [rewrite-clj.node.comment
            comment-node
            comment?]

           [rewrite-clj.node.extras
            whitespace-or-comment?]

           [rewrite-clj.node.fn
            fn-node]

           [rewrite-clj.node.forms
            forms-node]

           [rewrite-clj.node.integer
            integer-node]

           [rewrite-clj.node.keyword
            keyword-node
            keyword-node?]

           [rewrite-clj.node.meta
            meta-node
            raw-meta-node]

           [rewrite-clj.node.namespaced-map
            map-qualifier-node
            namespaced-map-node]

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
            set-node
            vector-node]

           [rewrite-clj.node.stringz
            string-node]

           [rewrite-clj.node.quote
            quote-node
            syntax-quote-node
            unquote-node
            unquote-splicing-node]

           [rewrite-clj.node.token
            token-node
            symbol-node?]

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
            whitespace-nodes]]}}
