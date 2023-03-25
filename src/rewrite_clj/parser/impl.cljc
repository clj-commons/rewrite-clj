(ns rewrite-clj.parser.impl
  {:no-doc true}
  (:require [rewrite-clj.reader :as reader])
  #?(:cljs (:import [goog.string StringBuffer])))

#?(:clj (set! *warn-on-reflection* true))

(defn flush-into
  "INTERNAL. Flush buffer and add string to the given vector."
  [lines ^StringBuffer buf]
  (let [s (.toString buf)]
    #?(:clj (.setLength buf 0) :cljs (.clear buf))
    (conj lines s)))

(defn read-string-data
  "INTERNAL."
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (reader/next reader)]
        (cond (and (not escape?) (identical? c \"))
              (flush-into lines buf)

              (identical? c \newline)
              (recur escape? (flush-into lines buf))

              :else
              (do
                (.append buf c)
                (recur (and (not escape?) (identical? c \\)) lines)))
        (reader/throw-reader reader "Unexpected EOF while reading string.")))))
