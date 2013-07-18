(ns ^{ :doc "Comment-/Whitespace-preserving EDN parser."}
  rewrite-clj.parser
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as r]
            [clojure.java.io :as io]
            [clojure.walk :as w]))

;; ## Readers

(defn string-reader
  "Create reader for strings."
  [s]
  (r/indexing-push-back-reader (r/string-push-back-reader s)))

(defn file-reader
  "Create reader for files."
  [f]
  (r/indexing-push-back-reader
    (r/input-stream-push-back-reader 
      (io/input-stream (io/file f)))))

;; ## Helpers

(defn- whitespace?
  "Check if a given character is a whitespace."
  [^java.lang.Character c]
  (and c (or (= c \,) (Character/isWhitespace c))))

(defn- token
  "Create tupel of [type value]."
  [type value]
  (vector type value))

(defn- read-next
  "Create token of the given type using the given read function."
  [type parse-fn reader]
  (token type (parse-fn reader)))

(defn- ignore
  "Ignore next character of Reader."
  [reader]
  (r/read-char reader)
  nil)

;; ## Parser

(def ^:private parse-table
  "Dispatch Table for Parsing."
  {\^ :meta      \# :sharp
   \( :list      \[ :vector    \{ :map
   \} :unmatched \] :unmatched \) :unmatched
   \~ :unquote   \' :quote     \` :syntax-quote
   \; :comment})

(defmulti ^:private parse-next
  "Parse the next element from the given reader. Dispatch is done using the first
   available character."
  (fn [reader delimiter] 
    (when-let [c (r/peek-char reader)]
      (cond (whitespace? c) :whitespace
            (= c delimiter) :matched
            :else (get parse-table c :token))))
  :default nil)

(defmethod parse-next nil [_ _] nil)
(defmethod parse-next :token [reader _] (read-next :token edn/read reader))
(defmethod parse-next :comment [reader _] (read-next :comment r/read-line reader))
(defmethod parse-next :matched [reader _] (ignore reader))
(defmethod parse-next :unmatched [reader _] (throw (Exception. "Unmatched Delimiter.")))

(defmethod parse-next :whitespace 
  [reader _] 
  (token 
    :whitespace 
    (loop [r []]
      (let [ws (r/peek-char reader)]
        (if-not (whitespace? ws)
          (apply str r)
          (do
            (ignore reader)
            (recur (conj r ws))))))))

(defmethod parse-next :meta 
  [reader delim]
  (token 
    :with-meta
    (do
      (ignore reader)
      (let [mta (parse-next reader delim)
            isq (repeatedly #(parse-next reader delim))
            spc (doall (take-while (comp #{:comment :whitespace} first) isq))
            vlu (nth isq (count spc))]
        (when-not vlu
          (throw (Exception. "Missing value for Metadata.")))
        (concat (list* mta spc) [vlu])))))

(defn- parse-delim
  [type delim reader]
  (ignore reader)
  (token 
    type 
    (doall
      (take-while 
        (complement nil?) 
        (repeatedly #(parse-next reader delim))))))

(defmethod parse-next :list [reader _] (parse-delim :list \) reader))
(defmethod parse-next :vector [reader _] (parse-delim :vector \] reader) )
(defmethod parse-next :map [reader _] (parse-delim :map \} reader) )

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

(defmethod parse-next :sharp
  [reader _]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (condp = c
      \{ (parse-delim :set \} reader)
      \" (parse-regex reader)
      \= (do
           (ignore reader)
           (token :eval (parse-next reader nil)))
      (do
        (r/unread reader \#)
        (token :token (edn/read reader))))))

(defmethod parse-next :unquote
  [reader _]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (if (= c \@)
      (do
        (ignore reader)
        (token :unquote-splicing (parse-next reader nil)))
      (token :unquote (parse-next reader nil)))))

(defmethod parse-next :quote
  [reader _]
  (ignore reader)
  (token :quote (parse-next reader nil)))

(defmethod parse-next :syntax-quote
  [reader _]
  (ignore reader)
  (token :syntax-quote (parse-next reader nil)))

;; ## Parse Wrapper

(defn parse-edn
  "Get next EDN tree from Reader."
  [reader]
  (parse-next reader nil))

(defn parse-edn-string
  "Get EDN tree from String."
  [s]
  (let [r (string-reader s)]
    (parse-edn r)))

(defn parse-edn-file
  "Get EDN tree from File."
  [f]
  (let [r (file-reader f)]
    (parse-edn r)))
