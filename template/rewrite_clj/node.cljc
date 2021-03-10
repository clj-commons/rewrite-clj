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
            value]

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
