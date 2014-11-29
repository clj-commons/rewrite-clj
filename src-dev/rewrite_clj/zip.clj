(ns rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.zip
             [base :as base]
             [find :as find]
             [move :as move]
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

  [rewrite-clj.zip.find
   find find-next
   find-tag find-next-tag
   find-value find-next-value
   find-token find-next-token]

  [rewrite-clj.zip.move
   left right up down prev next
   leftmost rightmost
   leftmost? rightmost? end?]

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

(defn ->string
  "DEPRECATED. Create string from current zipper location."
  [zloc]
  (string zloc))

(defn ->root-string
  "DEPRECATED. Zip up and create string from root node."
  [zloc]
  (root-string zloc))
