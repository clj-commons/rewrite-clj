(ns ^:no-doc rewrite-clj.parser.keyword
  (:require [clojure.tools.reader.edn :as edn]
            ;; internal tools reader namespaces to support read-keyword override work
            [clojure.tools.reader.impl.commons :as reader-impl-commons]
            [clojure.tools.reader.impl.errors :as reader-impl-errors]
            [clojure.tools.reader.impl.utils :as reader-impl-utils]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser.utils :as u] ))

#?(:clj (set! *warn-on-reflection* true))

(defn- read-keyword
  "This customized version of clojure.tools.reader.edn's read-keyword allows for
  an embedded `::` in a keyword to to support [garden-style keywords](https://github.com/noprompt/garden)
  like `:&::before`. This function was transcribed from clj-kondo."
  [reader]
  (let [ch (r/read-char reader)]
    (if-not (reader-impl-utils/whitespace? ch)
      (let [#?(:clj ^String token :default token) (#'edn/read-token reader :keyword ch)
            s (reader-impl-commons/parse-symbol token)]
        (if (and s
                 ;; (== -1 (.indexOf token "::")) becomes:
                 (not (zero? (.indexOf token "::"))))
          (let [#?(:clj ^String ns :default ns) (s 0)
                #?(:clj ^String name :default name) (s 1)]
            (if (identical? \: (nth token 0))
              (reader-impl-errors/throw-invalid reader :keyword token) ; No ::kw in edn.
              (keyword ns name)))
          (reader-impl-errors/throw-invalid reader :keyword token)))
      (reader-impl-errors/throw-single-colon reader))))

(defn parse-keyword
  [#?(:cljs ^not-native reader :default reader)]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (do
        (r/read-char reader)
        (node/keyword-node
         (read-keyword reader)
         true))
      (node/keyword-node (read-keyword reader)))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))
