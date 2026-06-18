(ns ^:no-doc rewrite-clj.parser.token
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.token :as ntoken]
            [rewrite-clj.reader :as r])
  #?@(:cljs [(:import [goog.string StringBuffer])]
      :default []))

#?(:clj (set! *warn-on-reflection* true))

;; Next two functions are extract to avoid allocating fn objects in refsites.

(defn- not-boundary? [c] (not (r/whitespace-or-boundary? c)))

(defn- not-boundary-allow-extra? [c]
  (or (#{\' \:} c)
      (not (r/whitespace-or-boundary? c))))

(defn- read-to-boundary
  [#?(:cljs ^not-native reader :default reader) buf f]
  (r/read-into-buffer-while reader buf f true))

(defn- read-to-char-boundary
  [#?(:cljs ^not-native reader :default reader)
   #?(:clj ^StringBuilder buf :default ^StringBuffer buf)]
  (if-let [c (r/next reader)]
    (do (.append buf (char c))
        (when-not (= c \\)
          (read-to-boundary reader buf not-boundary?)))
    ;; At least one char must be present after \
    (r/throw-reader reader "Unexpected EOF")))

(defn- symbol-node
  "Symbols allow for certain boundary characters that have
   to be handled explicitly."
  [#?(:cljs ^not-native reader :default reader) value value-string
   #?(:clj ^StringBuilder buf :default ^StringBuffer buf)]
  (let [length-before (#?(:clj .length :cljs .getLength) buf)
        _ (read-to-boundary reader buf not-boundary-allow-extra?)
        length-after (#?(:clj .length :cljs .getLength) buf)]
    (if (= length-before length-after)
      (ntoken/token-node value value-string)
      (let [s (str buf)]
        (ntoken/token-node (r/read-symbol s) s)))))

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
  (let [buf #?(:clj (StringBuilder.) :cljs (StringBuffer.))
        first-char (r/next reader)
        _ (.append buf (char first-char))
        _ (if (= first-char \\)
            (read-to-char-boundary reader buf)
            (read-to-boundary reader buf not-boundary?))
        s (str buf)
        v (if (or (= first-char \\) ;; character like \n or \newline
                  (= first-char \#) ;; something like ##Inf, ##Nan
                  (number-literal? s))
            (r/string->edn s)
            (r/read-symbol s))]
    (if (symbol? v)
      (symbol-node reader v s buf)
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
