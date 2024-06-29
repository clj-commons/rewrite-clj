(ns ^:no-doc rewrite-clj.parser.token
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.token :as ntoken]
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
          (r/read-symbol s)
          s)))))

(defn- number-literal?
  "Checks whether the reader is at the start of a number literal

  Cribbed and adapted from clojure.tools.reader.impl.commons"
  [[c1 c2]]
  (or (interop/numeric? c1)
      (and (or (identical? \+ c1) (identical? \- c1))
           (interop/numeric? c2))))

(defn parse-token
  "Parse a single token. For example: symbol, number or character."
  [#?(:cljs ^not-native reader :default reader)]
  (let [first-char (r/next reader)
        s (->> (if (= first-char \\)
                 (read-to-char-boundary reader)
                 (read-to-boundary reader))
               (str first-char))
        v (if (or (= first-char \\) ;; character like \n or \newline
                  (= first-char \#) ;; something like ##Inf, ##Nan
                  (number-literal? s))
            (r/string->edn s)
            (r/read-symbol s))]
    (if (symbol? v)
      (symbol-node reader v s)
      (ntoken/token-node v s))))

(comment
  (require '[clojure.tools.reader.reader-types :as rt])

  (parse-token (rt/string-push-back-reader "foo"))
  ;; => {:value foo, :string-value "foo", :map-qualifier nil}

  (parse-token (rt/string-push-back-reader "42"))
  ;; => {:value 42, :string-value "42"}

  (parse-token (rt/string-push-back-reader "+42"))
  ;; => {:value 42, :string-value "+42"}

  (parse-token (rt/string-push-back-reader "\\newline"))
  ;; => {:value \newline, :string-value "\\newline"}

  (parse-token (rt/string-push-back-reader "##Inf"))
  ;; => {:value ##Inf, :string-value "##Inf"}

  :eoc)
