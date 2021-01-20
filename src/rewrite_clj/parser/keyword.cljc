(ns ^:no-doc rewrite-clj.parser.keyword
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser.utils :as u]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-keyword
  [#?(:cljs ^not-native reader :default reader)]
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
