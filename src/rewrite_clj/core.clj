(ns ^{ :doc "rewrite-clj API facade."
       :author "Yannick Scherer" }
  rewrite-clj.core
  (:require [potemkin :refer [import-vars]]
            [rewrite-clj parser zip print]))

(import-vars
  [rewrite-clj.parser
   
   string-reader
   file-reader

   parse
   parse-string
   parse-file]
  
  [rewrite-clj.print
   
   print-edn])
