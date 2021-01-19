(ns ^:no-doc rewrite-clj.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.potemkin :refer [import-vars]]
            [rewrite-clj.zip.removez]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-clj.zip.removez
  remove
  remove-preserve-newline])
