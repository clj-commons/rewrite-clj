(ns ^:no-doc rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require #?@(:clj [[clojure.java.io :as io]])
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.interop :as interop])
  #?(:cljs (:import [goog.string StringBuffer])
     :clj (:import [java.io PushbackReader])))

;; ## Exception

(defn throw-reader
  "Throw reader exception, including line/column."
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
  #?(:clj ^Boolean [^java.lang.Character c]
     :default [c])
  (interop/clojure-whitespace? c))

(defn linebreak?
  [#?(:clj ^java.lang.Character c :default c)]
  (contains? #{\newline \return} c))

(defn space?
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
    (if-let [entry (read-fn reader)]
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

(defn string-reader
  "Create reader for strings."
  [s]
  (r/indexing-push-back-reader
    (r/string-push-back-reader s)))

#?(:clj
   (defn file-reader
     "Create reader for files."
     ;; TODO: import
     ^clojure.tools.reader.reader_types.IndexingPushbackReader
     [f]
     (-> (io/file f)
         (io/reader)
         (PushbackReader. 2)
         (r/indexing-push-back-reader 2))))
