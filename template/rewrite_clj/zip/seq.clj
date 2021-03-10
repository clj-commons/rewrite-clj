(ns ^:no-doc rewrite-clj.zip.seq
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.zip.seqz] ))

(set! *warn-on-reflection* true)

#_{:import-vars/import
   {:from [[rewrite-clj.zip.seqz
            seq?
            list?
            vector?
            set?
            map?
            map-vals
            map-keys
            map
            get
            assoc]]}}
