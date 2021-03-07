(ns ^:no-doc rewrite-clj.parser.keyword
  (:require [rewrite-clj.node.keyword :as nkeyword]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-keyword
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (if-let [c (reader/peek reader)]
    (if (= c \:)
      (do
        (reader/next reader)
        (nkeyword/keyword-node
         (reader/read-keyword reader)
         true))
      (nkeyword/keyword-node (reader/read-keyword reader)))
    (reader/throw-reader reader "unexpected EOF while reading keyword.")))
