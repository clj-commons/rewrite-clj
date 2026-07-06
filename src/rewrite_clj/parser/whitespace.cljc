(ns ^:no-doc rewrite-clj.parser.whitespace
  (:require [rewrite-clj.node.whitespace :as nwhitespace]
            [rewrite-clj.reader :as reader])
  #?@(:cljs [(:import [goog.string StringBuffer])]))

#?(:clj (set! *warn-on-reflection* true))

(def ^:private single-space-node (nwhitespace/whitespace-node " "))

(defn- parse-spaces
  [#?(:cljs ^not-native reader :default reader)]
  ;; Heuristic: most whitespace nodes are only one character long. Reuse the
  ;; same whitespace node object for all of them.
  (let [first-char (reader/next reader)]
    (if (reader/space? (reader/peek reader))
      (let [buf #?(:clj (StringBuilder.) :cljs (StringBuffer.))]
        (.append buf first-char)
        (reader/read-into-buffer-while reader buf reader/space? true)
        (nwhitespace/whitespace-node (str buf)))
      (if (= first-char \space)
        single-space-node
        (nwhitespace/whitespace-node (str first-char))))))

(def ^:private single-newline-node (nwhitespace/newline-node "\n"))

(defn- parse-linebreaks
  [#?(:cljs ^not-native reader :default reader)]
  ;; Same heuristic as for spaces.
  (let [first-char (reader/next reader)]
    (if (reader/linebreak? (reader/peek reader))
      (let [buf #?(:clj (StringBuilder.) :cljs (StringBuffer.))]
        (.append buf first-char)
        (reader/read-into-buffer-while reader buf reader/linebreak? true)
        (nwhitespace/newline-node (str buf)))
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
