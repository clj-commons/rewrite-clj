(ns rewrite-clj.potemkin-t2
  #?(:cljs (:require-macros [rewrite-clj.potemkin-t2])))

(def t-def 242)

(def t-def-doc "def with doc" 277)

(defn t-fn [arg] (+ 200 arg))

(defn t-fn-doc "function with doc" [arg] (+ 200 arg))

(defmacro t-macro [a] `(str "2" ~a))

(defmacro t-macro-doc "macro with doc" [a b c d] `(str "2" ~a ~b ~c ~d))
