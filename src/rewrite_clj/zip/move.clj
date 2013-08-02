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

(def next
  "Move to the next non-whitespace/non-comment location in a depth-first manner."
  (comp skip-whitespace z/next))

(def prev 
  "Move to the next non-whitespace/non-comment location in a depth-first manner."
  (comp skip-whitespace-left z/prev))

(def leftmost 
  "Move to the leftmost non-whitespace/non-comment location."
  (comp skip-whitespace z/leftmost))

(def rightmost 
  "Move to the rightmost non-whitespace/non-comment location."
  (comp skip-whitespace-left z/rightmost))
