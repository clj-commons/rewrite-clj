(ns rewrite-clj.node.regex
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord RegexNode [pattern]
  node/Node
  (tag [_]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_]
    false)
  (sexpr [_]
    (join-lines
      (map
        (comp edn/read-string wrap-string)
        lines)))
  (string [_]
    (wrap-string (join-lines lines)))
