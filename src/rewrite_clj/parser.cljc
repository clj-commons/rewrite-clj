(ns rewrite-clj.parser
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.parser.core :as p]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Parser Core

(defn parse
  "Parse next form from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (p/parse-next reader))

(defn parse-all
  "Parse all forms from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (let [nodes (->> (repeatedly #(parse reader))
                   (take-while identity)
                   (doall))]
    (with-meta
      (node/forms-node nodes)
      (meta (first nodes)))))

;; ## Specialized Parsers

(defn parse-string
  "Parse first form in the given string."
  [s]
  (parse (reader/string-reader s)))

(defn parse-string-all
  "Parse all forms in the given string."
  [s]
  (parse-all (reader/string-reader s)))

#?(:clj
   (defn parse-file
     "Parse first form from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse r)))))

#?(:clj
   (defn parse-file-all
     "Parse all forms from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse-all r)))))
