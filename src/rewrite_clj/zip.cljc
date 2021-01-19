(ns rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.potemkin :refer [import-vars]]
            [rewrite-clj.zip.base]
            [rewrite-clj.zip.edit]
            [rewrite-clj.zip.find]
            [rewrite-clj.zip.insert]
            [rewrite-clj.zip.move]
            [rewrite-clj.zip.remove]
            [rewrite-clj.zip.seq]
            [rewrite-clj.zip.subedit]
            [rewrite-clj.zip.walk]
            [rewrite-clj.zip.whitespace]))

;; ## API Facade

(import-vars
  [rewrite-clj.custom-zipper.core
   node position root]

  [rewrite-clj.zip.base
   child-sexprs
   edn* edn tag sexpr
   length value
   of-file of-string
   string root-string
   print print-root]

  [rewrite-clj.zip.edit
   replace edit splice
   prefix suffix]

  [rewrite-clj.zip.find
   find find-next
   find-depth-first
   find-next-depth-first
   find-tag find-next-tag
   find-value find-next-value
   find-token find-next-token]

  [rewrite-clj.zip.insert
   insert-right insert-left
   insert-child append-child]

  [rewrite-clj.zip.move
   left right up down prev next
   leftmost rightmost
   leftmost? rightmost? end?]

  [rewrite-clj.zip.remove
   remove]

  [rewrite-clj.zip.seq
   seq? list? vector? set? map?
   map map-keys map-vals
   get assoc]

  [rewrite-clj.zip.subedit
   edit-node edit-> edit->>
   subedit-node subedit-> subedit->>]

  [rewrite-clj.zip.walk
   prewalk
   postwalk]

  [rewrite-clj.zip.whitespace
   whitespace? linebreak?
   whitespace-or-comment?
   skip skip-whitespace
   skip-whitespace-left
   prepend-space append-space
   prepend-newline append-newline])

;; ## Base Operations

(defmacro ^:private defbase
  [sym base]
  (let [{:keys [arglists]} (meta
                             (ns-resolve
                               (symbol (namespace base))
                               (symbol (name base))))
        sym (with-meta
              sym
              {:doc (format "Directly call '%s' on the given arguments." base)
               :arglists `(quote ~arglists)})]
    `(def ~sym ~base)))

(defbase right* rewrite-clj.custom-zipper.core/right)
(defbase left* rewrite-clj.custom-zipper.core/left)
(defbase up* rewrite-clj.custom-zipper.core/up)
(defbase down* rewrite-clj.custom-zipper.core/down)
(defbase next* rewrite-clj.custom-zipper.core/next)
(defbase prev* rewrite-clj.custom-zipper.core/prev)
(defbase rightmost* rewrite-clj.custom-zipper.core/rightmost)
(defbase leftmost* rewrite-clj.custom-zipper.core/leftmost)
(defbase replace* rewrite-clj.custom-zipper.core/replace)
(defbase edit* rewrite-clj.custom-zipper.core/edit)
(defbase remove* rewrite-clj.custom-zipper.core/remove)
(defbase insert-left* rewrite-clj.custom-zipper.core/insert-left)
(defbase insert-right* rewrite-clj.custom-zipper.core/insert-right)

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))
