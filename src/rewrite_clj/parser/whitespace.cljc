(ns ^:no-doc rewrite-clj.parser.whitespace
  (:require [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [#?(:cljs ^not-native reader :default reader)]
  (let [c (reader/peek reader)]
    (cond (reader/linebreak? c)
          (nwhitespace/newline-node
            (reader/read-while reader reader/linebreak?))

          (reader/comma? c)
          (nwhitespace/comma-node
            (reader/read-while reader reader/comma?))

          :else
          (nwhitespace/whitespace-node
            (reader/read-while reader reader/space?)))))
