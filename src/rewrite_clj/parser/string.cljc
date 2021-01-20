(ns ^:no-doc rewrite-clj.parser.string
  (:require [clojure.string :as string]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser.utils :as u])
  #?(:cljs (:import [goog.string StringBuffer])))

#?(:clj (set! *warn-on-reflection* true))

(defn- flush-into
  "Flush buffer and add string to the given vector."
  [lines ^StringBuffer buf]
  (let [s (.toString buf)]
    #?(:clj (.setLength buf 0) :cljs (.clear buf))
    (conj lines s)))

(defn- read-string-data
  [#?(:cljs ^not-native reader :default reader)]
  (u/ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (r/read-char reader)]
        (cond (and (not escape?) (identical? c \"))
              (flush-into lines buf)

              (identical? c \newline)
              (recur escape? (flush-into lines buf))

              :else
              (do
                (.append buf c)
                (recur (and (not escape?) (identical? c \\)) lines)))
        (u/throw-reader reader "Unexpected EOF while reading string.")))))

(defn parse-string
  [#?(:cljs ^not-native reader :default reader)]
  (node/string-node (read-string-data reader)))

(defn parse-regex
  [#?(:cljs ^not-native reader :default reader)]
  (let [h (read-string-data reader)]
    (string/join "\n" h)))
