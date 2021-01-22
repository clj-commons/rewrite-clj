(ns ^:no-doc rewrite-clj.parser.core
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.parser.keyword :refer [parse-keyword]]
            [rewrite-clj.parser.namespaced-map :refer [parse-namespaced-map]]
            [rewrite-clj.parser.string :refer [parse-string parse-regex]]
            [rewrite-clj.parser.token :refer [parse-token]]
            [rewrite-clj.parser.whitespace :refer [parse-whitespace]]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Base Parser

(def ^:dynamic ^:private *delimiter*
  nil)

(defn- dispatch
  [c]
  (cond (nil? c)               :eof
        (reader/whitespace? c) :whitespace
        (= c *delimiter*)      :delimiter
        :else (get {\^ :meta      \# :sharp
                    \( :list      \[ :vector    \{ :map
                    \} :unmatched \] :unmatched \) :unmatched
                    \~ :unquote   \' :quote     \` :syntax-quote
                    \; :comment   \@ :deref     \" :string
                    \: :keyword}
                   c :token)))

(defmulti ^:private parse-next*
  (comp #'dispatch reader/peek))

(defn parse-next
  [#?(:cljs ^not-native reader :default reader)]
  (reader/read-with-meta reader parse-next*))

;; # Parser Helpers

(defn- parse-delim
  [#?(:cljs ^not-native reader :default reader) delimiter]
  (reader/ignore reader)
  (->> #(binding [*delimiter* delimiter]
          (parse-next %))
       (reader/read-repeatedly reader)))

(defn- parse-printables
  [#?(:cljs ^not-native reader :default reader) node-tag n & [ignore?]]
  (when ignore?
    (reader/ignore reader))
  (reader/read-n
    reader
    node-tag
    parse-next
    (complement node/printable-only?)
    n))

;; ## Parser Functions

;; ### Base

(defmethod parse-next* :token
  [#?(:cljs ^not-native reader :default reader)]
  (parse-token reader))

(defmethod parse-next* :delimiter
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader))

(defmethod parse-next* :unmatched
  [#?(:cljs ^not-native reader :default reader)]
  (reader/throw-reader
    reader
    "Unmatched delimiter: %s"
    (reader/peek reader)))

(defmethod parse-next* :eof
  [#?(:cljs ^not-native reader :default reader)]
  (when *delimiter*
    (reader/throw-reader reader "Unexpected EOF.")))

;; ### Whitespace

(defmethod parse-next* :whitespace
  [#?(:cljs ^not-native reader :default reader)]
  (parse-whitespace reader))

(defmethod parse-next* :comment
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (node/comment-node (reader/read-include-linebreak reader)))

;; ### Special Values

(defmethod parse-next* :keyword
  [#?(:cljs ^not-native reader :default reader)]
  (parse-keyword reader))

(defmethod parse-next* :string
  [#?(:cljs ^not-native reader :default reader)]
  (parse-string reader))

;; ### Meta

(defmethod parse-next* :meta
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (node/meta-node (parse-printables reader :meta 2)))

;; ### Reader Specialities

(defmethod parse-next* :sharp
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (case (reader/peek reader)
    nil (reader/throw-reader reader "Unexpected EOF.")
    \{ (node/set-node (parse-delim reader \}))
    \( (node/fn-node (parse-delim reader \)))
    \" (node/regex-node (parse-regex reader))
    \^ (node/raw-meta-node (parse-printables reader :meta 2 true))
    \' (node/var-node (parse-printables reader :var 1 true))
    \= (node/eval-node (parse-printables reader :eval 1 true))
    \_ (node/uneval-node (parse-printables reader :uneval 1 true))
    \: (parse-namespaced-map reader parse-next)
    \? (do
         ;; we need to examine the next character, so consume one (known \?)
         (reader/next reader)
         ;; we will always have a reader-macro-node as the result
         (node/reader-macro-node
          (let [read1 (fn [] (parse-printables reader :reader-macro 1))]
            (cons (case (reader/peek reader)
                    ;; the easy case, just emit a token
                    \( (node/token-node (symbol "?"))

                    ;; the harder case, match \@, consume it and emit the token
                    \@ (do (reader/next reader)
                           (node/token-node (symbol "?@")))

                    ;; otherwise no idea what we're reading but its \? prefixed
                    (do (reader/unread reader \?)
                        (first (read1))))
                  (read1)))))
    (node/reader-macro-node (parse-printables reader :reader-macro 2))))

(defmethod parse-next* :deref
  [#?(:cljs ^not-native reader :default reader)]
  (node/deref-node (parse-printables reader :deref 1 true)))

;; ## Quotes

(defmethod parse-next* :quote
  [#?(:cljs ^not-native reader :default reader)]
  (node/quote-node (parse-printables reader :quote 1 true)))

(defmethod parse-next* :syntax-quote
  [#?(:cljs ^not-native reader :default reader)]
  (node/syntax-quote-node (parse-printables reader :syntax-quote 1 true)))

(defmethod parse-next* :unquote
  [#?(:cljs ^not-native reader :default reader)]
  (reader/ignore reader)
  (let [c (reader/peek reader)]
    (if (= c \@)
      (node/unquote-splicing-node
        (parse-printables reader :unquote 1 true))
      (node/unquote-node
        (parse-printables reader :unquote 1)))))

;; ### Seqs

(defmethod parse-next* :list
  [#?(:cljs ^not-native reader :default reader)]
  (node/list-node (parse-delim reader \))))

(defmethod parse-next* :vector
  [#?(:cljs ^not-native reader :default reader)]
  (node/vector-node (parse-delim reader \])))

(defmethod parse-next* :map
  [#?(:cljs ^not-native reader :default reader)]
  (node/map-node (parse-delim reader \})))
