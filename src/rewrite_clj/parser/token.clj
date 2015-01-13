(ns ^:no-doc rewrite-clj.parser.token
  (:require [rewrite-clj
             [node :as node]
             [reader :as r]]))

(defn parse-token
  "Parse a single token."
  [reader]
  (let [s (r/read-until
            reader
            r/whitespace-or-boundary?)
        v (r/string->edn s)]
    (node/token-node v s)))
