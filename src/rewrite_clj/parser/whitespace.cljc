(ns ^:no-doc rewrite-clj.parser.whitespace
  (:require [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

(def ^:private single-space-node (nwhitespace/whitespace-node " "))

(defn- parse-spaces
  [#?(:cljs ^not-native reader :default reader)]
  ;; Heuristic: most whitespace nodes are only one character long. Reuse the
  ;; same whitespace node object for all of them.
  (let [first-char (reader/next reader)]
    (if (reader/space? (reader/peek reader))
      (do (reader/unread reader first-char)
          (nwhitespace/whitespace-node
           (reader/read-while reader reader/space?)))
      (if (= first-char \space)
        single-space-node
        (nwhitespace/whitespace-node (str first-char))))))

(def ^:private single-newline-node (nwhitespace/newline-node "\n"))

(defn- parse-linebreaks
  [#?(:cljs ^not-native reader :default reader)]
  ;; Same heuristic as for spaces.
  (let [first-char (reader/next reader)]
    (if (reader/linebreak? (reader/peek reader))
      (do (reader/unread reader first-char)
          (nwhitespace/newline-node (reader/read-while reader reader/linebreak?)))
      (if (= first-char \newline)
        single-newline-node
        (nwhitespace/newline-node (str first-char))))))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [#?(:cljs ^not-native reader :default reader)]
  (let [c (reader/peek reader)]
    (cond (reader/linebreak? c)
          (parse-linebreaks reader)

          (reader/comma? c)
          (nwhitespace/comma-node
            (reader/read-while reader reader/comma?))

          :else (parse-spaces reader))))
