(ns ^:no-doc rewrite-clj.parser.whitespace
  (:require [rewrite-clj
             [node :as node]
             [reader :as reader]]))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [reader]
  (if (reader/linebreak? (reader/peek reader))
    (node/newline-node
      (reader/read-while reader reader/linebreak?))
    (node/whitespace-node
      (reader/read-while reader reader/space?))))
