;; DO NOT EDIT FILE, automatically generated from: template/rewrite_clj/node/string.clj
(ns ^:no-doc rewrite-clj.node.string
  "This ns exists to preserve compatability for rewrite-clj v0 clj users who were using an internal API.
   This ns does not work for cljs due to namespace collisions."
  (:require [rewrite-clj.node.stringz]))

(set! *warn-on-reflection* true)


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.node.stringz
(defn string-node
  "Create node representing a string value where `lines`
   can be a sequence of strings or a single string."
  [lines] (rewrite-clj.node.stringz/string-node lines))
