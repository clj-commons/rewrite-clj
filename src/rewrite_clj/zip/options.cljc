(ns ^:no-doc rewrite-clj.zip.options
  (:require [rewrite-clj.node.protocols :as protocols]))

#?(:clj (set! *warn-on-reflection* true))

(def default-zipper-opts
  {:track-position? false
   :auto-resolve protocols/default-auto-resolve})

(defn get-opts [zloc]
  (:rewrite-clj.zip/opts (meta zloc)))

(defn set-opts [zloc opts]
  (vary-meta zloc assoc :rewrite-clj.zip/opts (merge default-zipper-opts opts)))
