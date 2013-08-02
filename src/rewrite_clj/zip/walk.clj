(ns ^{ :doc "Walk Operations over Zipper"
       :author "Yannick Scherer" }
  rewrite-clj.zip.walk
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip.core :as c :only [subzip]]
            [rewrite-clj.zip.move :as m :only [next]]))

(defn prewalk
  "Perform a depth-first pre-order traversal starting at the given zipper location
   and apply the given function to each child node. If a predicate `p?` is given, 
   only apply the function to nodes matching it."
  ([zloc f] (prewalk zloc (constantly true) f))
  ([zloc p? f]
   (loop [loc (c/subzip zloc)
          prv loc]
     (if-let [n0 (find loc m/next p?)]
       (if-let [n1 (f n0)]
         (recur (m/next n1) n1)
         (recur (m/next n0) n0))
       (z/replace zloc (z/root prv))))))
