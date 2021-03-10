(ns ^:no-doc rewrite-clj.zip.remove
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.zip.removez]))

(set! *warn-on-reflection* true)

#_{:import-vars/import
   {:from [[rewrite-clj.zip.removez
            remove
            remove-preserve-newline]]}}
