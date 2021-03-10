(ns ^:no-doc rewrite-clj.zip.move
  (:refer-clojure :exclude [next])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

(defn right
  "Return zipper with location moved right to next non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc]
  (some-> zloc zraw/right ws/skip-whitespace))

(defn left
  "Return zipper with location moved left to next non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc]
  (some-> zloc zraw/left ws/skip-whitespace-left))

(defn down
  "Return zipper with location moved down to the first non-whitespace/non-comment child node of the current node in `zloc`, or nil if no applicable children."
  [zloc]
  (some-> zloc zraw/down ws/skip-whitespace))

(defn up
  "Return zipper with location moved up to next non-whitespace/non-comment parent of current node in `zloc`, or `nil` if at the top."
  [zloc]
  (some-> zloc zraw/up ws/skip-whitespace-left))

(defn next
  "Return zipper with location moved to the next depth-first non-whitespace/non-comment node in `zloc`.
   End can be detected with [[end?]], if already at end, stays there."
  [zloc]
  (when zloc
    (or (some->> zloc
                 zraw/next
                 (ws/skip-whitespace zraw/next))
        (vary-meta zloc assoc ::end? true))))

(defn end?
  "Return true if `zloc` is at end of depth-first traversal."
  [zloc]
  (or (not zloc)
      (zraw/end? zloc)
      (::end? (meta zloc))))

(defn rightmost?
  "Return true if at rightmost non-whitespace/non-comment sibling node in `zloc`."
  [zloc]
  (nil? (ws/skip-whitespace (zraw/right zloc))))

(defn leftmost?
  "Return true if at leftmost non-whitespace/non-comment sibling node in `zloc`."
  [zloc]
  (nil? (ws/skip-whitespace-left (zraw/left zloc))))

(defn prev
  "Return zipper with location moved to the previous depth-first non-whitespace/non-comment node in `zloc`. If already at root, returns nil."
  [zloc]
  (some->> zloc
           zraw/prev
           (ws/skip-whitespace zraw/prev)))

(defn leftmost
  "Return zipper with location moved to the leftmost non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc]
  (some-> zloc
          zraw/leftmost
          ws/skip-whitespace))

(defn rightmost
  "Return zipper with location moved to the rightmost non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc]
  (some-> zloc
          zraw/rightmost
          ws/skip-whitespace-left))
