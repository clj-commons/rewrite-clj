(ns ^:no-doc rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require #?@(:clj [[clojure.java.io :as io]])
            [clojure.tools.reader.edn :as edn]
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
  (r/peek-char reader))

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

;; ## Reader Types

;;
;; clojure.tools.reader (at the time of this writing v1.3.5) does not seem to normalize Windows \r\n newlines
;; properly to \n for Clojure 
;;
;; ClojureScript seems to work fine - but note that for peek it can return \r instead of \n. 
;;
;; see https://clojure.atlassian.net/browse/TRDR-65
;;
;; For now, we introduce a normalizing reader for Clojure.
;; Once/if this isssue is fixed in in tools reader we can turf our work-around. 

#?(:clj
   (deftype NewlineNormalizingReader
       [rdr
        ^:unsynchronized-mutable next-char
        ^:unsynchronized-mutable peeked-char]
     r/Reader
     (read-char [_reader]
       (if peeked-char
         (let [ch peeked-char]
           (set! peeked-char nil)
           ch)
         (let [ch (or next-char (r/read-char rdr))]
           (when next-char (set! next-char nil))
           (cond (identical? \return ch)
                 (let [next-ch (r/read-char rdr)]
                   (when (not (identical? \newline next-ch))
                     (set! next-char next-ch))
                   \newline)

                 (identical? \formfeed ch)
                 \newline

                 :else
                 ch))))

     (peek-char [reader]
       (let [ch (or peeked-char (.read-char reader))]
         (when-not peeked-char (set! peeked-char ch))
         ch))))

#?(:clj
   (defn ^Closeable newline-normalizing-reader
     "Normalizes the following line endings to LF (line feed - 0x0A):
      - LF (remains LF)
      - CRLF (carriage return 0x0D line feed 0x0A)
      - FF (form feed 0x0C)"
     [rdr]
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
