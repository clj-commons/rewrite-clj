(ns ^:no-doc rewrite-clj.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [rewrite-clj.potemkin.clojure :refer [import-vars]]
            [rewrite-clj.zip.editz]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-clj.zip.editz
  replace
  edit
  splice
  prefix
  suffix])
