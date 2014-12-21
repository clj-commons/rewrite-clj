(ns rewrite-clj.parser.keyword
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.parser.utils :as u]
            [clojure.tools.reader
             [edn :as edn]
             [reader-types :as r]]))

(defn parse-keyword
  [reader]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (node/keyword-node
        (edn/read reader)
        true)
      (do
        (r/unread reader \:)
        (node/keyword-node (edn/read reader))))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))
