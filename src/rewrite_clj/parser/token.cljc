(ns ^:no-doc rewrite-clj.parser.token
  (:require [rewrite-clj.node.token :as ntoken]
            [rewrite-clj.reader :as r]))

#?(:clj (set! *warn-on-reflection* true))

(defn- read-to-boundary
  [#?(:cljs ^not-native reader :default reader) & [allowed]]
  (let [allowed? (set allowed)]
    (r/read-until
      reader
      #(and (not (allowed? %))
            (r/whitespace-or-boundary? %)))))

(defn- read-to-char-boundary
  [#?(:cljs ^not-native reader :default reader)]
  (let [c (r/next reader)]
    (str c
         (if (not= c \\)
           (read-to-boundary reader)
           ""))))

(defn- symbol-node
  "Symbols allow for certain boundary characters that have
   to be handled explicitly."
  [#?(:cljs ^not-native reader :default reader) value value-string]
  (let [suffix (read-to-boundary
                 reader
                 [\' \:])]
    (if (empty? suffix)
      (ntoken/token-node value value-string)
      (let [s (str value-string suffix)]
        (ntoken/token-node
          (r/string->edn s)
          s)))))

(defn parse-token
  "Parse a single token."
  [#?(:cljs ^not-native reader :default reader)]
  (let [first-char (r/next reader)
        s (->> (if (= first-char \\)
                 (read-to-char-boundary reader)
                 (read-to-boundary reader))
               (str first-char))
        v (r/string->edn s)]
    (if (symbol? v)
      (symbol-node reader v s)
      (ntoken/token-node v s))))
