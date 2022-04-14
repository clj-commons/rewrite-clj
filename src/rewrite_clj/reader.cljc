(ns ^:no-doc rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require #?@(:clj [[clojure.java.io :as io]])
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.impl.commons :as reader-impl-commons]
            [clojure.tools.reader.impl.errors :as reader-impl-errors]
            [clojure.tools.reader.impl.utils :as reader-impl-utils]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.interop :as interop])
  #?(:cljs (:import [goog.string StringBuffer])
     :clj (:import [java.io PushbackReader Closeable]
                   [clojure.tools.reader.reader_types IndexingPushbackReader])))

#?(:clj (set! *warn-on-reflection* true))

;; ## Exception

(defn throw-reader
  "Throw reader exception, including line line/column."
  [#?(:cljs ^:not-native reader :default reader) fmt & data]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw
     (ex-info
      (str (apply interop/simple-format fmt data)
           " [at line " l ", column " c "]") {}))))

;; ## Decisions

(defn boundary?
  "Check whether a given char is a token boundary."
  [#?(:clj ^java.lang.Character c :default c)]
  (contains?
    #{\" \: \; \' \@ \^ \` \~
      \( \) \[ \] \{ \} \\ nil}
    c))

(defn comma?
  [#?(:clj ^java.lang.Character c :default c)]
  (identical? \, c))

(defn whitespace?
  "Checks whether a given character is whitespace"
  #?(:clj ^Boolean [^java.lang.Character c]
     :default [c])
  (interop/clojure-whitespace? c))

(defn linebreak?
  "Checks whether the character is a newline"
  [#?(:clj ^java.lang.Character c :default c)]
  (contains? #{\newline \return} c))

(defn space?
  "Checks whether the character is a space"
  [#?(:clj ^java.lang.Character c :default c)]
  (and c
       (whitespace? c)
       (not (contains? #{\newline \return \,} c))))

(defn whitespace-or-boundary?
  #?(:clj ^Boolean [^java.lang.Character c]
          :default [c])
  (or (whitespace? c) (boundary? c)))

;; ## Helpers

(defn read-while
  "Read while the chars fulfill the given condition. Ignores
    the unmatching char."
  ([#?(:cljs ^not-native reader :default reader) p?]
   (read-while reader p? (not (p? nil))))

  ([#?(:cljs ^not-native reader :default reader) p? eof?]
   (let [buf (StringBuffer.)]
     (loop []
       (if-let [c (r/read-char reader)]
         (if (p? c)
           (do
             (.append buf c)
             (recur))
           (do
             (r/unread reader c)
             (.toString buf)))
         (if eof?
           (.toString buf)
           (throw-reader reader "unexpected EOF")))))))

(defn read-until
  "Read until a char fulfills the given condition. Ignores the
   matching char."
  [#?(:cljs ^not-native reader :default reader) p?]
  (read-while
    reader
    (complement p?)
    (p? nil)))

(defn read-include-linebreak
  "Read until linebreak and include it."
  [#?(:cljs ^not-native reader :default reader)]
  (str
    (read-until
      reader
      #(or (nil? %) (linebreak? %)))
    (r/read-char reader)))

(defn string->edn
  "Convert string to EDN value."
  [#?(:clj ^String s :default s)]
  (edn/read-string s))

(defn ignore
  "Ignore the next character."
  [#?(:cljs ^not-native reader :default reader)]
  (r/read-char reader)
  nil)

(defn next
  "Read next char."
  [#?(:cljs ^not-native reader :default reader)]
  (r/read-char reader))

(defn unread
  "Unreads a char. Puts the char back on the reader."
  [#?(:cljs ^not-native reader :default reader) ch]
  (r/unread reader ch))

(defn peek
  "Peek next char."
  [#?(:cljs ^not-native reader :default reader)]
  (let [ch (r/peek-char reader)]
    ;; compensate for cljs newline normalization in tools reader v1.3.5
    ;; see https://clojure.atlassian.net/browse/TRDR-65
    (if (identical? \return ch)
      \newline
      ch)))

(defn position
  "Create map of `row-k` and `col-k` representing the current reader position."
  [#?(:cljs ^not-native reader :default reader) row-k col-k]
  {row-k (r/get-line-number reader)
   col-k (r/get-column-number reader)})

(defn read-with-meta
  "Use the given function to read value, then attach row/col metadata."
  [#?(:cljs ^not-native reader :default reader) read-fn]
  (let [start-position (position reader :row :col)]
    (when-let [entry (read-fn reader)]
      (->> (position reader :end-row :end-col)
           (merge start-position)
           (with-meta entry)))))

(defn read-repeatedly
  "Call the given function on the given reader until it returns
   a non-truthy value."
  [#?(:cljs ^not-native reader :default reader) read-fn]
  (->> (repeatedly #(read-fn reader))
       (take-while identity)
       (doall)))

(defn read-n
  "Call the given function on the given reader until `n` values matching `p?` have been
   collected."
  [#?(:cljs ^not-native reader :default reader) node-tag read-fn p? n]
  {:pre [(pos? n)]}
  (loop [c 0
         vs []]
    (if (< c n)
      (if-let [v (read-fn reader)]
        (recur
          (if (p? v) (inc c) c)
          (conj vs v))
        (throw-reader
          reader
          "%s node expects %d value%s."
          node-tag
          n
          (if (= n 1) "" "s")))
      vs)))

;;
;; ## Customizations
;;
(defn read-keyword
  "This customized version of clojure.tools.reader.edn's read-keyword allows for
  an embedded `::` in a keyword to to support [garden-style keywords](https://github.com/noprompt/garden)
  like `:&::before`. This function was transcribed from clj-kondo."
  [reader]
  (let [ch (r/read-char reader)]
    (if-not (reader-impl-utils/whitespace? ch)
      (let [#?(:clj ^String token :default token) (#'edn/read-token reader :keyword ch)
            s (reader-impl-commons/parse-symbol token)]
        (if (and s
                 ;; (== -1 (.indexOf token "::")) becomes:
                 (not (zero? (.indexOf token "::"))))
          (let [#?(:clj ^String ns :default ns) (s 0)
                #?(:clj ^String name :default name) (s 1)]
            (if (identical? \: (nth token 0))
              (reader-impl-errors/throw-invalid reader :keyword token) ; No ::kw in edn.
              (keyword ns name)))
          (reader-impl-errors/throw-invalid reader :keyword token)))
      (reader-impl-errors/throw-single-colon reader))))

;; ## Reader Types

;;
;; clojure.tools.reader (at the time of this writing v1.3.5) does not seem to normalize Windows \r\n newlines
;; properly to \n for Clojure
;;
;; ClojureScript seems to work fine - but note that for peek it will return \r for \r\n and \r\f instead of \n.
;;
;; see https://clojure.atlassian.net/browse/TRDR-65
;;
;; For now, we introduce a normalizing reader for Clojure.
;; Once/if this isssue is fixed in in tools reader we can turf our work-around.

#?(:clj
   (deftype NewlineNormalizingReader
       [rdr
        ^:unsynchronized-mutable read-ahead-char
        ^:unsynchronized-mutable user-peeked-char]
     r/Reader
     (read-char [_reader]
       (if-let [ch user-peeked-char]
         (do (set! user-peeked-char nil) ch)
         (let [ch (or read-ahead-char (r/read-char rdr))]
           (when read-ahead-char (set! read-ahead-char nil))
           (if (not (identical? \return ch))
             ch
             (let [read-ahead-ch (r/read-char rdr)]
               (when (not (or (identical? \newline read-ahead-ch)
                              (identical? \formfeed read-ahead-ch)))
                 (set! read-ahead-char read-ahead-ch))
               \newline)))))

     (peek-char [reader]
       (or user-peeked-char
           (let [ch (.read-char reader)]
             (set! user-peeked-char ch)
             ch)))))

#?(:clj
   (defn newline-normalizing-reader
     "Normalizes the following line endings to LF (line feed - 0x0A):
      - LF (remains LF)
      - CRLF (carriage return 0x0D line feed 0x0A)
      - CRFF (carriage return 0x0D form feed 0x0C)"
     ^Closeable [rdr]
     (NewlineNormalizingReader. (r/to-rdr rdr) nil nil)))

#?(:clj
   (defn file-reader
     "Create reader for files."
     ^IndexingPushbackReader
     [f]
     (-> (io/file f)
         (io/reader)
         (PushbackReader. 2)
         newline-normalizing-reader
         (r/indexing-push-back-reader 2))))

(defn string-reader
  "Create reader for strings."
  [s]
  (-> s
      r/string-push-back-reader
      #?@(:clj [newline-normalizing-reader])
      r/indexing-push-back-reader))
