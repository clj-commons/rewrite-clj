(ns ^:no-doc rewrite-clj.parser.utils
  (:require [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.interop :as interop]))

#?(:clj (set! *warn-on-reflection* true))

(defn whitespace?
  "Check if a given character is a whitespace."
  [#?(:clj ^java.lang.Character c :default c)]
  (interop/clojure-whitespace? c))

(defn linebreak?
  "Check if a given character is a linebreak."
  [#?(:clj ^java.lang.Character c :default c)]
  (and c (or (= c \newline) (= c \return))))

(defn space?
  "Check if a given character is a non-linebreak whitespace."
  [#?(:clj ^java.lang.Character c :default c)]
  (and (not (linebreak? c)) (whitespace? c)))

(defn ignore
  "Ignore next character of Reader."
  [#?(:cljs ^not-native reader :default reader)]
  (r/read-char reader)
  nil)

(defn throw-reader
  [#?(:cljs ^not-native reader :default reader) & msg]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw (ex-info
            (str (apply str msg) " [at line " l ", column " c "]") {}))))

(defn read-eol
  [#?(:cljs ^not-native reader :default reader)]
  (loop [char-seq []]
    (if-let [c (r/read-char reader)]
      (if (linebreak? c)
        (apply str (conj char-seq c))
        (recur (conj char-seq c)))
      (apply str char-seq))))
