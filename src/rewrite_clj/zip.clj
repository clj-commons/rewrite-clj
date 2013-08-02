(ns ^{ :doc "Zipper Utilities for EDN Trees." 
       :author "Yannick Scherer" } 
  rewrite-clj.zip
  (:refer-clojure :exclude [replace next remove find 
                            map get assoc
                            seq? vector? list? map? set?
                            print])
  (:require [potemkin :refer [import-vars]]
            [fast-zip.core :as z]
            [rewrite-clj.convert :as conv]
            [rewrite-clj.parser :as p]
            [rewrite-clj.printer :as prn])
  (:require rewrite-clj.zip.core
            rewrite-clj.zip.edit
            rewrite-clj.zip.move
            rewrite-clj.zip.find
            rewrite-clj.zip.seqs
            rewrite-clj.zip.walk))

;; ## Import

(import-vars
  [fast-zip.core 
   node root]

  [rewrite-clj.zip.core 

   edn tag value sexpr
   whitespace? linebreak? 
   skip-whitespace skip-whitespace-left
   prepend-space append-space
   prepend-newline append-newline
   edit-> subzip]

  [rewrite-clj.zip.move
   
   left right up down prev next
   leftmost rightmost]
  
  [rewrite-clj.zip.find
   
   find find-next
   find-tag find-next-tag
   find-value find-next-value
   find-token find-next-token]

  [rewrite-clj.zip.edit
   
   insert-right insert-left
   insert-child append-child
   replace edit remove splice]

  [rewrite-clj.zip.seqs
   
   seq? map? vector? list? set?
   map map-keys get assoc]

  [rewrite-clj.zip.walk
   
   prewalk])

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

;; ## Convenience Functions

(defn of-string
  "Create zipper from String."
  [s]
  (when-let [tree (p/parse-string-all s)]
    (edn tree)))

(defn of-file
  "Create zipper from File."
  [f]
  (when-let [tree (p/parse-file-all f)]
    (edn tree)))

(defn print
  "Print current zipper location."
  [zloc]
  (-> zloc z/node prn/print-edn))

(defn print-root
  "Zip up and print root node."
  [zloc]
  (-> zloc z/root prn/print-edn))

(defn ->string
  "Create string from current zipper location."
  [zloc]
  (-> zloc z/node prn/->string))

(defn ->root-string
  "Zip up and create string from root node."
  [zloc]
  (-> zloc z/root prn/->string))
