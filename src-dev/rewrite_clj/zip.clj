(ns rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.zip
             [base :as base]
             [edit :as edit]
             [find :as find]
             [insert :as insert]
             [move :as move]
             [remove :as remove]
             [seq :as seq]
             [subedit :as subedit]
             [whitespace :as ws]]
            [rewrite-clj
             [parser :as p]
             [node :as node]]
            [fast-zip.core :as z]
            [potemkin :refer [import-vars]]))

;; ## API Facade

(import-vars
  [fast-zip.core
   node root]

  [rewrite-clj.zip.base
   edn* edn tag sexpr
   of-file of-string
   string root-string
   print print-root]

  [rewrite-clj.zip.edit
   replace edit splice
   prefix suffix]

  [rewrite-clj.zip.find
   find find-next
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

  [rewrite-clj.zip.whitespace
   whitespace? linebreak?
   whitespace-or-comment?
   skip skip-whitespace
   skip-whitespace-left
   prepend-space append-space
   prepend-newline append-newline])

;; ## Base Operations

(def right* z/right)
(def left* z/left)
(def up* z/up)
(def down* z/down)
(def next* z/next)
(def prev* z/prev)
(def rightmost* z/rightmost)
(def leftmost* z/leftmost)
(def replace* z/replace)
(def edit* z/edit)
(def remove* z/remove)

;; ## DEPRECATED

(defn ^:deprecated ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^:deprecated ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))

(defn ^:deprecated value
  "DEPRECATED. Use `sexpr` instead."
  [zloc]
  (sexpr zloc))
