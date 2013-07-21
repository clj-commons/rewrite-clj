(ns ^{ :doc "Parser Utilities" 
       :author "Yannick Scherer" }
  rewrite-clj.parser.utils
  (:require [clojure.tools.reader.reader-types :as r :only [read-char get-line-number get-column-number]]))

(defn whitespace?
  "Check if a given character is a whitespace."
  [^java.lang.Character c]
  (and c (or (= c \,) (Character/isWhitespace c))))

(defn token
  "Create tupel of [type value]."
  [type & values]
  (vec (list* type values)))

(defn read-next
  "Create token of the given type using the given read function."
  [type parse-fn reader]
  (token type (parse-fn reader)))

(defn ignore
  "Ignore next character of Reader."
  [reader]
  (r/read-char reader)
  nil)

(defn throw-reader
  [reader & msg]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw
      (Exception.
        (str (apply str msg) " [at line " l ", column " c "]")))))
