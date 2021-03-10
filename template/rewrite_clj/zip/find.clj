(ns ^:no-doc rewrite-clj.zip.find
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [find])
  (:require [rewrite-clj.zip.findz]))

(set! *warn-on-reflection* true)

#_{:import-vars/import
   {:from [[rewrite-clj.zip.findz
            find
            find-last-by-pos
            find-depth-first
            find-next
            find-next-depth-first
            find-tag
            find-next-tag
            find-tag-by-pos
            find-token
            find-next-token
            find-value
            find-next-value]]}}
