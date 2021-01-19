(ns ^:no-doc rewrite-clj.zip.seq
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.potemkin.clojure :refer [import-vars]]
            [rewrite-clj.zip.seqz] ))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-clj.zip.seqz
  seq?
  list?
  vector?
  set?
  map?
  map-seq
  map-vals
  map-keys
  map
  get
  assoc])
