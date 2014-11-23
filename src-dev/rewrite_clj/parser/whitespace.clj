(ns rewrite-clj.parser.whitespace
  (:require [rewrite-clj.parser.utils :as u]
            [rewrite-clj.node :as node]
            [clojure.tools.reader.reader-types :as r]))

(defn- parse-while
  [reader p?]
  (loop [char-seq []]
    (if (p? (r/peek-char reader))
      (recur (conj char-seq (r/read-char reader)))
      (apply str char-seq))))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [reader]
  (if (u/linebreak? (r/peek-char reader))
    (node/newline-node
      (parse-while reader u/linebreak?))
    (node/whitespace-node
      (parse-while reader u/space?))))
