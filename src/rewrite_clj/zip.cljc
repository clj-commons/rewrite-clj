(ns rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.custom-zipper.core]
            #?(:clj [rewrite-clj.potemkin.clojure :refer [import-vars import-vars-with-mods]])
            [rewrite-clj.zip.base]
            [rewrite-clj.zip.context]
            [rewrite-clj.zip.editz]
            [rewrite-clj.zip.findz]
            [rewrite-clj.zip.insert]
            [rewrite-clj.zip.move]
            [rewrite-clj.zip.removez]
            [rewrite-clj.zip.seqz]
            [rewrite-clj.zip.subedit #?@(:cljs [:include-macros true])]
            [rewrite-clj.zip.walk]
            [rewrite-clj.zip.whitespace])
  #?(:cljs (:require-macros [rewrite-clj.potemkin.cljs :refer [import-vars import-vars-with-mods]]
                            [rewrite-clj.zip])))

#?(:clj (set! *warn-on-reflection* true))

;; ## API Facade

(import-vars
  [rewrite-clj.custom-zipper.core
   node position position-span root]

  [rewrite-clj.zip.base
   child-sexprs
   edn* edn
   tag sexpr
   length
   value
   #?(:clj of-file)
   of-string
   string root-string
   print print-root]

  [rewrite-clj.zip.editz
   replace edit splice
   prefix suffix]

  [rewrite-clj.zip.context
   reapply-context]

  [rewrite-clj.zip.findz
   find find-next
   find-depth-first
   find-next-depth-first
   find-tag find-next-tag
   find-value find-next-value
   find-token find-next-token
   find-last-by-pos
   find-tag-by-pos]

  [rewrite-clj.zip.insert
   insert-right insert-left
   insert-child append-child]

  [rewrite-clj.zip.move
   left right up down prev next
   leftmost rightmost
   leftmost? rightmost? end?]

  [rewrite-clj.zip.removez
   remove]

  [rewrite-clj.zip.seqz
   seq? list? vector? set? map? namespaced-map?
   map map-keys map-vals
   get assoc]

  [rewrite-clj.zip.subedit
   edit-node
   subedit-node
   subzip
   edit-> edit->>
   subedit-> subedit->>]

  [rewrite-clj.zip.walk
   prewalk
   postwalk]

  [rewrite-clj.zip.whitespace
   whitespace? linebreak?
   whitespace-or-comment?
   skip skip-whitespace
   skip-whitespace-left
   insert-space-left insert-space-right
   insert-newline-left insert-newline-right
   prepend-space append-space
   prepend-newline append-newline])

(import-vars-with-mods
 {:sym-to-pattern "@@orig-name@@*"
  :doc-to-pattern "Raw version of [[@@orig-name@@]].\n\n@@orig-doc@@\n\nNOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."}
 [rewrite-clj.custom-zipper.core
  right left up down
  next prev
  rightmost leftmost
  replace edit remove
  insert-left insert-right
  insert-child
  append-child])

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))
