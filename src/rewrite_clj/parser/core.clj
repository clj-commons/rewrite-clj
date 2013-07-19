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
   \; :comment})

(defmulti parse-next
  "Parse the next element from the given reader. Dispatch is done using the first
   available character."
  (fn [reader delimiter] 
    (when-let [c (r/peek-char reader)]
      (cond (whitespace? c) :whitespace
            (= c delimiter) :matched
            :else (get parse-table c :token))))
  :default nil)

;; ## Simple Tokens

(defmethod parse-next nil 
  [_ _] 
  (throw (Exception. "Unknown Token Type.")))

(defmethod parse-next :token [reader _] (read-next :token edn/read reader))
(defmethod parse-next :comment [reader _] (read-next :comment r/read-line reader))
(defmethod parse-next :matched [reader _] (ignore reader))
(defmethod parse-next :unmatched [reader _] (throw (Exception. "Unmatched Delimiter.")))

;; ## Whitespace

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

;; ## Metadata

(defmethod parse-next :meta 
  [reader delim]
  (apply token 
    :meta
    (do
      (ignore reader)
      (let [mta (parse-next reader delim)
            isq (repeatedly #(parse-next reader delim))
            spc (doall (take-while (comp #{:comment :whitespace} first) isq))
            vlu (nth isq (count spc))]
        (when-not vlu
          (throw (Exception. "Missing value for Metadata.")))
        (concat (list* mta spc) [vlu])))))

;; ## Seqs

(defn- parse-delim
  [type delim reader]
  (ignore reader)
  (apply token 
    type 
    (doall
      (take-while 
        (complement nil?) 
        (repeatedly #(parse-next reader delim))))))

(defmethod parse-next :list [reader _] (parse-delim :list \) reader))
(defmethod parse-next :vector [reader _] (parse-delim :vector \] reader) )
(defmethod parse-next :map [reader _] (parse-delim :map \} reader) )

;; ## Regular Expressions/Sets

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

;; ## Quotes

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
