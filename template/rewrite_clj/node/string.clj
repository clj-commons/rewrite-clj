(ns ^:no-doc rewrite-clj.node.string
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:require [rewrite-clj.node.stringz]))

(set! *warn-on-reflection* true)

#_{:import-vars/import
   {:from [[rewrite-clj.node.stringz
            string-node]]}}
