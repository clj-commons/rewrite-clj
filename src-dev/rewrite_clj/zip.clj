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
   edn* edn tag sexpr]

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

;; ## Zipper Constructors

(defn of-string
  "Create zipper from String."
  [s]
  (some-> s p/parse-string-all edn))

(defn of-file
  "Create zipper from File."
  [f]
  (some-> f p/parse-file-all edn))

(defn string
  "Create string representing the current zipper location."
  [zloc]
  (some-> zloc z/node node/string))

(defn root-string
  "Create string representing the zipped-up zipper."
  [zloc]
  (some-> zloc z/root node/string))

(defn print
  "Print current zipper location."
  [zloc]
  (some-> zloc string print))

(defn print-root
  "Zip up and print root node."
  [zloc]
  (some-> zloc z/root string print))

;; ## DEPRECATED

(defn ->string
  "DEPRECATED. Create string from current zipper location."
  [zloc]
  (string zloc))

(defn ->root-string
  "DEPRECATED. Zip up and create string from root node."
  [zloc]
  (root-string zloc))
