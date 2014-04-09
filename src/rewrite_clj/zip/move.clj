(ns ^{ :doc "Whitespace/Comment aware movement through zipper."
       :author "Yannick Scherer" }
  rewrite-clj.zip.move
  (:refer-clojure :exclude [next])
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip.core :refer [skip-whitespace skip-whitespace-left]]))

;; ### Move

(def right
  "Move right to next non-whitespace/non-comment location."
  (comp skip-whitespace z/right))

(def left
  "Move left to next non-whitespace/non-comment location."
  (comp skip-whitespace-left z/left))

(def down
  "Move down to next non-whitespace/non-comment location."
  (comp skip-whitespace z/down))

(def up
  "Move up to next non-whitespace/non-comment location."
  (comp skip-whitespace-left z/up))

(defn next
  "Move to the next non-whitespace/non-comment location in a depth-first manner."
  [loc]
  (or (->> (z/next loc) (skip-whitespace z/next))
      (vary-meta loc assoc ::end? true)))

(defn end?
  "Check whether the given node is at the end of the depth-first traversal."
  [loc]
  (or (z/end? loc) (::end? (meta loc))))

(def prev
  "Move to the next non-whitespace/non-comment location in a depth-first manner."
  (comp (partial skip-whitespace z/prev) z/prev))

(def leftmost
  "Move to the leftmost non-whitespace/non-comment location."
  (comp skip-whitespace z/leftmost))

(def rightmost
  "Move to the rightmost non-whitespace/non-comment location."
  (comp skip-whitespace-left z/rightmost))
