(ns ^:no-doc rewrite-clj.parser.whitespace
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [reader]
  (let [c (reader/peek reader)]
    (cond (reader/linebreak? c)
          (node/newline-node
            (reader/read-while reader reader/linebreak?))

          (reader/comma? c)
          (node/comma-node
            (reader/read-while reader reader/comma?))

          :else
          (node/whitespace-node
            (reader/read-while reader reader/space?)))))
