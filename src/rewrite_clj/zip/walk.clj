(ns ^:no-doc rewrite-clj.zip.walk
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip
             [subedit :refer [subedit-node]]
             [move :as m]]))

(defn- prewalk-subtree
  [p? f zloc]
  (loop [loc zloc]
    (if (m/end? loc)
      loc
      (if (p? loc)
        (if-let [n (f loc)]
          (recur (m/next n))
          (recur (m/next loc)))
        (recur (m/next loc))))))

(defn prewalk
  "Perform a depth-first pre-order traversal starting at the given zipper location
   and apply the given function to each child node. If a predicate `p?` is given,
   only apply the function to nodes matching it."
  ([zloc f] (prewalk zloc (constantly true) f))
  ([zloc p? f]
   (->> (partial prewalk-subtree p? f)
        (subedit-node zloc))))
