(ns ^:no-doc rewrite-clj.parser.string
  (:require [clojure.string :as string]
            [rewrite-clj.node.stringz :as nstring]
            [rewrite-clj.parser.impl :as pimpl]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-string
  [#?(:cljs ^not-native reader :default reader)]
  (nstring/string-node (pimpl/read-string-data reader)))

(defn parse-regex
  [#?(:cljs ^not-native reader :default reader)]
  (let [h (pimpl/read-string-data reader)]
    (string/join "\n" h)))
