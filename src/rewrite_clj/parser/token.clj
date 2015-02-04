(ns ^:no-doc rewrite-clj.parser.token
  (:require [rewrite-clj
             [node :as node]
             [reader :as r]]))

(defn- read-to-boundary
  [reader]
  (r/read-until
    reader
    r/whitespace-or-boundary?))

(defn- read-to-char-boundary
  [reader]
  (let [c (r/next reader)]
    (str c
         (if (not= c \\)
           (read-to-boundary reader)
           ""))))

(defn- symbol-node
  "Symbols allow for trailing quotes that have to be handled
   explicitly."
  [reader value value-string]
  (if (= (r/peek reader) \')
    (let [s (str value-string (r/next reader))]
      (node/token-node
        (r/string->edn s)
        s))
    (node/token-node value value-string)))

(defn parse-token
  "Parse a single token."
  [reader]
  (let [first-char (r/next reader)
        s (->> (if (= first-char \\)
                 (read-to-char-boundary reader)
                 (read-to-boundary reader))
               (str first-char))
        v (r/string->edn s)]
    (if (symbol? v)
      (symbol-node reader v s)
      (node/token-node v s))))
