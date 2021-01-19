(ns ^:no-doc rewrite-clj.node.string
  (:require [rewrite-clj.node.stringz]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-clj.node.stringz
  string-node])
