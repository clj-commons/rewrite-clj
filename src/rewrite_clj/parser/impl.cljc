(ns rewrite-clj.parser.impl
  {:no-doc true}
  (:require [rewrite-clj.reader :as reader])
  #?(:cljs (:import [goog.string StringBuffer])))

#?(:clj (set! *warn-on-reflection* true))

(defn- make-buffer []
  #?(:clj (StringBuilder.) :cljs (StringBuffer.)))

(defn read-string-data
  "INTERNAL."
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (loop [escape? false, lines [], buf (make-buffer)]
    (if-let [c (reader/next reader)]
      (cond (and (not escape?) (identical? c \"))
            (conj lines (str buf))

            (identical? c \newline)
            (recur escape? (conj lines (str buf)) (make-buffer))

            :else
            (do #?(:clj (.append ^StringBuilder buf (char c))
                   :cljs (.append ^StringBuffer buf (char c)))
                (recur (and (not escape?) (identical? c \\)) lines buf)))
      (reader/throw-reader reader "Unexpected EOF while reading string."))))
