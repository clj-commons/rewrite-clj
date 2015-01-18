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

(defn parse-token
  "Parse a single token."
  [reader]
  (let [first-char (r/next reader)
        s (->> (if (= first-char \\)
                 (read-to-char-boundary reader)
                 (read-to-boundary reader))
               (str first-char))
        v (r/string->edn s)]
    (node/token-node v s)))
