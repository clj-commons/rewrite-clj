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
   \; :comment   \@ :deref     \" :string
   \: :keyword})

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
  [type reader delim]
  (let [p (r/read-char reader)] 
    (if-let [t (parse-next reader delim)]
    (token type t)
    (throw-reader reader "'" type "' expects a value following the prefix '\\" p "'."))))

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
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [reader]
  (let [c (r/read-char reader)
        [t p?] (if (linebreak? c) [:newline linebreak?] [:whitespace space?])]
    (token t 
           (loop [r [c]]
             (if-not (p? (r/peek-char reader))
               (apply str r)
               (recur (conj r (r/read-char reader))))))))

(defn- parse-pair
  "Parse two tokens (and whitespace inbetween)."
  [type reader delim]
  (let [c (r/read-char reader)]
    (if-let [mta (parse-next reader delim)]
      (let [isq (repeatedly #(parse-next reader nil))
            spc (doall (take-while (comp #{:comment :whitespace :newline} first) isq))
            vlu (nth isq (count spc))]
        (when-not vlu (throw-reader reader "'" type "' expects two values after prefix '" c "'."))
        (apply token type (concat (list* mta spc) [vlu])))
      (throw-reader reader "'" type "' expects a value after the prefix '" c "'."))))

(defn- parse-regex
  "Parse regular expression string. The first available character has to be the
   opening quotation mark."
  [reader]
  (ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false]
      (if-let [c (r/read-char reader)]
        (if (and (not escape?) (= c \")) 
          (token :token (re-pattern (.toString buf)))
          (do 
            (.append buf c)
            (recur (and (not escape?) (= c \\)))))
        (throw-reader reader "Unexpected EOF while reading regular expression.")))))

(defn- parse-reader-macro
  "Parse token starting with '#'."
  [reader delim]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (condp = c
      \{ (parse-delimited :set \} reader)
      \( (parse-delimited :fn \) reader)
      \" (parse-regex reader)
      \' (parse-prefixed :var reader delim)
      \^ (parse-pair :meta* reader delim)
      \= (parse-prefixed :eval reader delim)
      (do (r/unread reader \#) (parse-pair :reader-macro reader delim)))))

(defn- parse-unquote
  "Parse token starting with '~'."
  [reader delim]
  (ignore reader)
  (let [c (r/peek-char reader)]
    (if (= c \@)
      (parse-prefixed :unquote-splicing reader delim)
      (do
        (r/unread reader \~)
        (parse-prefixed :unquote reader delim)))))

(defn- parse-string-contents
  "Use EDN reader to transform the literal representation of a Clojure string
   to its Clojure representation, e.g. \"a\\\\nb\" -> \"a\\nb\"."
  [s]
  (edn/read-string (str "\"" s "\"")))

(defn- parse-string
  "Parse string. Produces `:token` for single-line strings and `:multi-line` for
   those spanning multiple lines."
  [reader delim]
  (ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false
           result-type :token
           results []]
      (if-let [c (r/read-char reader)]
        (cond (and (not escape?) (= c \")) (->> (conj results (.toString buf))
                                             (map parse-string-contents)
                                             (apply token result-type))
              (= c \newline) (let [s (.toString buf)]
                               (.setLength buf 0)
                               (recur escape? :multi-line (conj results s)))
              :else (do 
                      (.append buf c)
                      (recur (and (not escape?) (= c \\)) result-type results)))
        (throw-reader reader "Unexpected EOF while reading regular expression.")))))

(defn- parse-keyword 
  [reader]
  (ignore reader)
  (let [nxt (edn/read reader)]
    (cond (keyword? nxt) [:namespaced nxt]
          (symbol? nxt) [:token (keyword nxt)]
          :else (throw-reader "Invalid token(s) following ':' prefix: " nxt))))

;; ## Register Parsers

(defmethod parse-next nil [reader delim]
  (if-let [c (r/peek-char reader)]
    (throw-reader reader "Cannot parse value starting with '" c  "'.")
    (when delim (throw-reader reader "Unexpected EOF (expected '" delim "')"))))

(defmethod parse-next :unmatched [reader _]   
  (throw-reader reader "Unmatched delimiter '" (r/peek-char reader) "'."))

(defmethod parse-next :token [reader _]        (read-next :token edn/read reader))
(defmethod parse-next :comment [reader _]      (read-next :comment r/read-line reader))
(defmethod parse-next :matched [reader _]      (ignore reader))
(defmethod parse-next :deref [reader delim]    (parse-prefixed :deref reader delim))
(defmethod parse-next :whitespace [reader _]   (parse-whitespace reader))
(defmethod parse-next :meta [reader delim]     (parse-pair :meta reader delim))
(defmethod parse-next :list [reader _]         (parse-delimited :list \) reader))
(defmethod parse-next :vector [reader _]       (parse-delimited :vector \] reader) )
(defmethod parse-next :map [reader _]          (parse-delimited :map \} reader) )
(defmethod parse-next :sharp [reader delim]    (parse-reader-macro reader delim))
(defmethod parse-next :unquote [reader delim]  (parse-unquote reader delim)) 
(defmethod parse-next :quote [reader delim]    (parse-prefixed :quote reader delim))
(defmethod parse-next :syntax-quote [reader d] (parse-prefixed :syntax-quote reader d))
(defmethod parse-next :string [reader delim]   (parse-string reader delim)) 
(defmethod parse-next :keyword [reader delim]  (parse-keyword reader))
