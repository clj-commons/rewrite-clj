(ns ^{ :doc "Comment-/Whitespace-preserving EDN parser."
       :author "Yannick Scherer" }
  rewrite-clj.parser.core
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.parser.utils :refer :all]))

;; ## Base

(def ^:private parse-table
  "Dispatch Table for Parsing."
  {\^ :meta      \# :sharp
   \( :list      \[ :vector    \{ :map
   \} :unmatched \] :unmatched \) :unmatched
   \~ :unquote   \' :quote     \` :syntax-quote
   \; :comment   \@ :deref})

(defmulti parse-next
  "Parse the next element from the given reader. Dispatch is done using the first
   available character. If the given delimiter is reached, nil shall be returned."
  (fn [reader delimiter] 
    (when-let [c (r/peek-char reader)]
      (cond (whitespace? c) :whitespace
            (= c delimiter) :matched
            :else (get parse-table c :token))))
  :default nil)

(defn- parse-prefixed
  "Ignore the first available char and parse the next token of the given type."
  [type reader]
  (ignore reader)
  (token type (parse-next reader nil)))

(defn- parse-delimited
  "Parse until the given delimiter is reached."
  [type delim reader]
  (ignore reader)
  (apply token 
    type 
    (doall
      (take-while 
        (complement nil?) 
        (repeatedly #(parse-next reader delim))))))

(defn- parse-whitespace
  "Parse as much whitespace as possible."
  [reader]
  (token 
    :whitespace 
    (loop [r []]
      (let [ws (r/peek-char reader)]
        (if-not (whitespace? ws)
          (apply str r)
          (do
            (ignore reader)
            (recur (conj r ws))))))))

(defn- parse-meta
  "Parse Token starting with '^'"
  [type reader]
  (ignore reader)
  (apply token type
         (let [mta (parse-next reader nil)
               isq (repeatedly #(parse-next reader nil))
               spc (doall (take-while (comp #{:comment :whitespace} first) isq))
               vlu (nth isq (count spc))]
           (when-not vlu
             (throw (Exception. "Missing value for Metadata.")))
           (concat (list* mta spc) [vlu]))))

(defn- parse-regex
  "Parse regular expression string. The first available character has to be the
   opening quotation mark."
  [reader]
  (ignore reader)
  (loop [rx []
         escape? false]
    (let [c (r/read-char reader)]
      (cond (and (not escape?) (= c \")) (token :token (re-pattern (apply str rx)))
            (= c \\) (recur (conj rx c) true)
            :else (recur (conj rx c) false)))))

(defn- parse-reader-macro
  "Parse token starting with '#'."
  [reader]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (condp = c
      \{ (parse-delimited :set \} reader)
      \( (parse-delimited :fn \) reader)
      \" (parse-regex reader)
      \' (parse-prefixed :var reader)
      \^ (parse-meta :meta* reader)
      \= (parse-prefixed :eval reader)
      (do (r/unread reader \#) (token :token (edn/read reader))))))

(defn- parse-unquote
  "Parse token starting with '~'."
  [reader]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (if (= c \@)
      (parse-prefixed :unquote-splicing reader)
      (token :unquote (parse-next reader nil)))))

;; ## Register Parsers

(defmethod parse-next nil [reader _] (throw (Exception. "Unknown Token Type.")))

(defmethod parse-next :token [reader _]        (read-next :token edn/read reader))
(defmethod parse-next :comment [reader _]      (read-next :comment r/read-line reader))
(defmethod parse-next :matched [reader _]      (ignore reader))
(defmethod parse-next :unmatched [reader _]    (throw (Exception. "Unmatched Delimiter.")))
(defmethod parse-next :deref [reader _]        (parse-prefixed :deref reader))
(defmethod parse-next :whitespace [reader _]   (parse-whitespace reader))
(defmethod parse-next :meta [reader delim]     (parse-meta :meta reader))
(defmethod parse-next :list [reader _]         (parse-delimited :list \) reader))
(defmethod parse-next :vector [reader _]       (parse-delimited :vector \] reader) )
(defmethod parse-next :map [reader _]          (parse-delimited :map \} reader) )
(defmethod parse-next :sharp [reader _]        (parse-reader-macro reader))
(defmethod parse-next :unquote [reader _]      (parse-unquote reader)) 
(defmethod parse-next :quote [reader _]        (parse-prefixed :quote reader))
(defmethod parse-next :syntax-quote [reader _] (parse-prefixed :syntax-quote reader))
