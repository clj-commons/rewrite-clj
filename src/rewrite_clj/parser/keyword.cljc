(ns ^:no-doc rewrite-clj.parser.keyword
  (:require [rewrite-clj.node.invalid :as invalid]
            [rewrite-clj.node.keyword :as nkeyword]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-keyword
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (if-let [c (reader/peek reader)]
    (if (= c \:)
      (do
        (reader/next reader)
        (if (reader/peek reader)
          (nkeyword/keyword-node
           (reader/read-keyword reader)
           true)
          (invalid/invalid-keyword-node)))
      (nkeyword/keyword-node (reader/read-keyword reader)))
    (reader/throw-reader reader "unexpected EOF while reading keyword.")))

(defn safe-parse-keyword
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (let [v (reader/lax-read-keyword reader)]
    (if (v 4)
      (nkeyword/keyword-node
       (keyword (v 1) (v 3))
       (v 0))
      (invalid/invalid-keyword-node
       (v 0) (v 1) (v 2) (v 3)))) )
