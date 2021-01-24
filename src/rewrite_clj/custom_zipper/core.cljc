;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;functional hierarchical zipper, with navigation, editing and enumeration
;see Huet

(ns ^:no-doc ^{:doc "Functional hierarchical zipper, with navigation, editing,
  and enumeration.  See Huet.
  Modified to optionally support row col position tracking."
       :author "Rich Hickey"}
  rewrite-clj.custom-zipper.core
  (:refer-clojure :exclude (replace remove next))
  (:require [clojure.zip :as clj-zip]
            [rewrite-clj.custom-zipper.switchable :refer [defn-switchable]]
            [rewrite-clj.node.protocols :as node])
  #?(:cljs (:require-macros [rewrite-clj.custom-zipper.switchable :refer [defn-switchable]])))

#?(:clj (set! *warn-on-reflection* true))

;; ## Switch
;;
;; To not force users into using this custom zipper, the following flag
;; is used to dispatch to `clojure.zip` when set to `false`.

(defn ^:no-doc custom-zipper
  [root]
  {::custom? true
   :node     root
   :position [1 1]
   :parent   nil
   :left     []
   :right   '()})

(defn ^:no-doc zipper
  [root]
  (clj-zip/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    root))

(defn ^:no-doc custom-zipper?
  [value]
  (::custom? value))

;; ## Implementation

(defn-switchable node
  "Returns the current node in `zloc`."
  [zloc]
  (:node zloc))

(defn-switchable branch?
  "Returns true if the current node in `zloc` is a branch."
  [zloc]
  (node/inner? (:node zloc)))

(defn-switchable children
  "Returns a seq of the children of current node in `zloc`, which must be a branch."
  [{:keys [node] :as zloc}]
  (if (branch? zloc)
    (seq (node/children node))
    (throw (ex-info "called children on a leaf node" {}))))

(defn-switchable ^:no-doc make-node
  "Returns a new branch node, given an existing `node` and new
  `children`. "
  [_zloc node children]
  (node/replace-children node children))

(defn position
  "Returns the ones-based `[row col]` of the start of the current node in `zloc`."
  [zloc]
  (if (custom-zipper? zloc)
    (:position zloc)
    (throw
     (ex-info
      (str "to use position functions, please construct your zipper with "
           "':track-position?'  set to true.") {}))))

(defn position-span
  "Returns the ones-based `[[start-row start-col] [end-row end-col]]` of the current node in `zloc`."
  [zloc]
  (let [start-pos (position zloc)]
    [start-pos (node/+extent start-pos (node/extent (node zloc)))]))


(defn-switchable lefts
  "Returns a seq of the left siblings of current node in `zloc`."
  [zloc]
  (map first (:left zloc)))

(defn-switchable down
  "Returns zipper with the location at the leftmost child of current node in `zloc`, or
  nil if no children."
  [zloc]
  (when (branch? zloc)
    (let [{:keys [node] [row col] :position} zloc
          [c & cnext :as cs] (children zloc)]
      (when cs
        {::custom? true
         :node     c
         :position [row (+ col (node/leader-length node))]
         :parent   zloc
         :left     []
         :right    cnext}))))

(defn-switchable up
  "Returns zipper with the location at the parent of current node in `zloc`, or nil if at
  the top."
  [zloc]
  (let [{:keys [node parent left right changed?]} zloc]
    (when parent
      (if changed?
        (assoc parent
               :changed? true
               :node (make-node zloc
                                (:node parent)
                                (concat (map first left) (cons node right))))
        parent))))

(defn-switchable root
  "Zips all the way up `zloc` and returns zipper at the root node, reflecting any changes."
  [{:keys [end?] :as zloc}]
  (if end?
    (node zloc)
    (let [p (up zloc)]
      (if p
        (recur p)
        (node zloc)))))

(defn-switchable right
  "Returns zipper with location at the right sibling of the current node in `zloc`, or nil."
  [zloc]
  (let [{:keys [node parent position left] [r & rnext :as right] :right} zloc]
    (when (and parent right)
      (assoc zloc
             :node r
             :left (conj left [node position])
             :right rnext
             :position (node/+extent position (node/extent node))))))

(defn-switchable rightmost
  "Returns zipper with location at the rightmost sibling of the current node in `zloc`, or self."
  [zloc]
  (if-let [next (right zloc)]
    (recur next)
    zloc))

(defn-switchable left
  "Returns zipper with location at the left sibling of the current node in `zloc`, or nil."
  [zloc]
  (let [{:keys [node parent left right]} zloc]
    (when (and parent (seq left))
      (let [[lnode lpos] (peek left)]
        (assoc zloc
               :node lnode
               :position lpos
               :left (pop left)
               :right (cons node right))))))

(defn-switchable leftmost
  "Returns zipper with location at the leftmost sibling of the current node in `zloc`, or self."
  [zloc]
  (let [{:keys [node parent left right]} zloc]
    (if (and parent (seq left))
      (let [[lnode lpos] (first left)]
        (assoc zloc
               :node lnode
               :position lpos
               :left []
               :right (concat (map first (rest left)) [node] right)))
      zloc)))

(defn-switchable insert-left
  "Returns zipper with `item` inserted as the left sibling of current node in `zloc`,
 without moving location."
  [zloc item]
  (let [{:keys [parent position left]} zloc]
    (if-not parent
      (throw (ex-info "cannot insert left at top" {}))
      (assoc zloc
             :changed? true
             :left (conj left [item position])
             :position (node/+extent position (node/extent item))))))

(defn-switchable insert-right
  "Returns zipper with `item` inserted as the right sibling of the current node in `zloc`,
  without moving location."
  [zloc item]
  (let [{:keys [parent right]} zloc]
    (if-not parent
      (throw (ex-info "cannot insert right at top" {}))
      (assoc zloc
             :changed? true
             :right (cons item right)))))

(defn-switchable replace
  "Returns zipper with `node` replacing current node in `zloc`, without moving location."
  [zloc node]
  (assoc zloc :changed? true :node node))

(defn edit
  "Returns zipper with value of `(f current-node args)` replacing current node in `zloc`"
  [zloc f & args]
  (if (custom-zipper? zloc)
    (replace zloc (apply f (node zloc) args))
    (apply clj-zip/edit zloc f args)))

(defn-switchable insert-child
  "Returns zipper with `item` inserted as the leftmost child of the current node in `zloc`,
  without moving location."
  [zloc item]
  (replace zloc (make-node zloc (node zloc) (cons item (children zloc)))))

(defn-switchable append-child
  "Returns zipper with `item` inserted as the rightmost child of the current node in `zloc`,
  without moving."
  [zloc item]
  (replace zloc (make-node zloc (node zloc) (concat (children zloc) [item]))))

(defn-switchable next
  "Returns zipper with location at the next depth-first location in the hierarchy in `zloc`.
  When reaching the end, returns a distinguished zipper detectable via [[end?]]. If already
  at the end, stays there."
  [{:keys [end?] :as zloc}]
  (if end?
    zloc
    (or
     (and (branch? zloc) (down zloc))
     (right zloc)
     (loop [p zloc]
       (if (up p)
         (or (right (up p)) (recur (up p)))
         (assoc p :end? true))))))

(defn-switchable prev
  "Returns zipper with location at the previous depth-first location in the hierarchy in `zloc`.
  If already at the root, returns nil."
  [zloc]
  (if-let [lloc (left zloc)]
    (loop [zloc lloc]
      (if-let [child (and (branch? zloc) (down zloc))]
        (recur (rightmost child))
        zloc))
    (up zloc)))

(defn-switchable end?
  "Returns true if at end of depth-first walk in `zloc`."
  [zloc]
  (:end? zloc))

(defn-switchable remove
  "Returns zipper with current node in `zloc` removed, with location at node that would have preceded
  it in a depth-first walk."
  [zloc]
  (let [{:keys [parent left right]} zloc]
    (if-not parent
      (throw (ex-info "cannot remove at top" {}))
      (if (seq left)
        (loop [zloc (let [[lnode lpos] (peek left)]
                     (assoc zloc
                            :changed? true
                            :position lpos
                            :node lnode
                            :left (pop left)))]
          (if-let [child (and (branch? zloc) (down zloc))]
            (recur (rightmost child))
            zloc))
        (assoc parent
               :changed? true
               :node (make-node zloc (:node parent) right))))))
