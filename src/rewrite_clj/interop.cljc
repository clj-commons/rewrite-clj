(ns ^:no-doc rewrite-clj.interop
  #?(:cljs (:require [goog.string :as gstring]
                     goog.string.format)))

#?(:clj (set! *warn-on-reflection* true))

(defn simple-format
  "Interop version of string format
  Note that there a big differences between Java's format and Google Closure's format - we don't address them.
  %d and %s are known to work in both."
  [template & args]
  #?(:clj (apply format template args)
     :cljs (apply gstring/format template args)))
