(ns ^:no-doc rewrite-clj.node.indent
  (:require [clojure.string :as string]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as ws #?@(:cljs [:include-macros true])]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Helpers

(defn- add-to-lines
  [all? s lines]
  (if all?
    (string/replace lines #"\r?\n" #(str % s))
    (string/replace
      lines #"(\r?\n)([^\r\n]*)$"
      (fn [[_ nl rst]]
        (str nl s rst)))))

;; ## Nodes

(defrecord LinePrefixedNode [child prefix prefix-length prefix-all?]
  node/Node
  (tag [_]
    (node/tag child))
  (printable-only? [_]
    (node/printable-only? child))
  (sexpr [_]
    (node/sexpr child))
  (length [this]
    ;; FIXME: directly calculate length
    (count (node/string this)))
  (string [_]
    (ws/with-newline-fn
      #(add-to-lines prefix-all? prefix %)
      (str prefix (node/string child))))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! LinePrefixedNode)

;; ## Constructors

(defn prefix-lines
  [node prefix]
  (->LinePrefixedNode node prefix (count prefix) true))

(defn indent-spaces
  [node n]
  (let [prefix (apply str (repeat n \space))]
    (->LinePrefixedNode node prefix n false)))

(defn indent-tabs
  [node n]
  (let [prefix (apply str (repeat n \tab))]
    (->LinePrefixedNode node prefix n false)))
