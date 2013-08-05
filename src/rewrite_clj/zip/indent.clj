(ns ^{ :doc "Zipper operations that take care of adjusting indentation."
       :author "Yannick Scherer"}
  rewrite-clj.zip.indent
  (:refer-clojure :exclude [replace remove])
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip.core :as zc]
            [rewrite-clj.zip.edit :as ze]
            [rewrite-clj.zip.find :as zf]
            [rewrite-clj.printer :as p :only [estimate-length]]))

;; ## Propagate Identation
;;
;; If a node is changed, multi-line elements to the right of it might have
;; to adjust identation. For example, the following example changes `keyword`
;; to `k`.
;;
;;     (lookup keyword {:one 1
;;                      :two 2})
;;
;; becomes
;;
;;     (lookup k {:one 1
;;                :two 2})
;;

;; ## Helpers

(defn- whitespace-length
  "Get width of whitespace node. Tabulator is treated as width 4."
  [zloc]
  (let [[k data] (z/node zloc)]
    (if-not (= k :whitespace) 0
      (->> data
        (map
          (fn [c]
            (case c
              \space 1
              \tab   4
              0)))
        (apply +)))))

(defn- remove-spaces-after
  "Remove spaces after the given node."
  [zloc delta]
  (loop [loc zloc
         delta delta]
    (let [rloc (z/right loc)]
      (if (or (pos? delta) (not rloc) (not= (zc/tag rloc) :whitespace))
        loc
        (let [w (whitespace-length rloc)
              delta (+ delta w)]
          (if-not (pos? delta)
            (recur (-> rloc z/remove) delta)
            (recur (-> rloc (zc/append-space delta) z/remove) delta)))))))

(defn- remove-spaces-before
  "Remove spaces before the given node."
  [zloc delta]
  (loop [loc zloc
         delta delta]
    (let [lloc (z/left loc)]
      (if (or (pos? delta) (not lloc) (not= (zc/tag lloc) :whitespace))
        loc
        (let [w (whitespace-length lloc)
              delta (+ delta w)]
          (cond (zero? delta) (-> lloc z/remove z/next)
                (not (pos? delta)) (recur (-> lloc z/remove z/next) delta)
                :else (-> lloc (zc/prepend-space delta) z/remove z/next)))))))

(defn- apply-indent
  "Apply the given indentation (positive: add spaces, negative: remove spaces) to the given node.
   If the given node is a newline node, spaces will be added/removed after it; otherwise, before it."
  [zloc delta]
  (cond (zero? delta) zloc
        (pos? delta) (if (zc/linebreak? zloc)
                       (zc/append-space zloc delta)
                       (zc/prepend-space zloc delta))
        :else (if (zc/linebreak? zloc)
                (remove-spaces-after zloc delta)
                (remove-spaces-before zloc delta))))

(defn- maximum-negative-indent
  "Find the maximum number of spaces (at most `delta`) to remove until either the beginning of the
   document or a non-`:whitespace` node is encountered."
  [zloc delta]
  (loop [zloc (z/prev zloc)
         c 0]
    (cond (< c delta) delta
          (or (not zloc) (zc/linebreak? zloc) (= (zc/tag zloc) :forms)) (max c delta)
          (not= (zc/tag zloc) :whitespace) (max (inc c) delta)
          :else (recur (z/prev zloc) (- c (whitespace-length zloc))))))

(defn- find-node-to-indent
  "Searches for a multi-line node right of the given location (starting on
   the same line). If there is none, `nil` is returned."
  [zloc]
  (loop [zloc zloc]
    (when-not (or (not zloc) (z/end? zloc) (zc/linebreak? zloc)) 
      (if-not (z/branch? zloc)
        (recur (z/right zloc))
        (if (zf/find-tag zloc z/next :newline)
          zloc
          (recur (z/right zloc)))))))

;; ## Low-Level Indentation

(defn indent-children
  "Indent the given node's children by the given number of spaces. If `delta` is
   negative, spaces will be removed."
  [zloc delta]
  (if (or (zero? delta) (not (z/branch? zloc)) (not (z/down zloc)))
    zloc
    (loop [loc (-> zloc zc/subzip z/down)]
      (cond (z/end? loc) (z/replace zloc (z/root loc))
            (zc/linebreak? loc) (recur (z/next (apply-indent loc delta)))
            :else (recur (z/next loc))))))

(defn indent
  "Indent the given node - including all children - by the given number of spaces. If `delta`
   is negative, spaces will be removed."
  [zloc delta]
  (let [delta (if (neg? delta) 
                (maximum-negative-indent zloc delta)
                delta)]
    (-> zloc 
      (apply-indent delta) 
      (indent-children delta))))

;; ## Modify + Indent

(defn- modify-and-indent
  [f zloc & args]
  (let [rloc (apply f zloc args)
        w0 (zc/length zloc)
        w1 (zc/length rloc)
        delta (- w1 w0)]
    (if (zero? delta) rloc 
      (if-let [iloc (find-node-to-indent rloc)]
        (-> iloc (indent-children delta) (zc/move-to-node zloc))
        rloc))))

(def replace 
  "Replace the sexpr at the given zipper location and indent accordingly."
  (partial modify-and-indent ze/replace))

(def edit 
  "Edit the sexpr at the given zipper location and indent accordingly."
  (partial modify-and-indent ze/edit))

;; ## Insert + Indent
;;
;; The insert function should return the inserted element.

(defn insert-left
  "Insert the given sexpr to the left of the given zipper location and indent
   accordingly."
  [zloc v]
  (let [rloc (ze/insert-left zloc v)
        delta (inc (zc/length (-> rloc z/left z/left)))]
    (if (zero? delta) rloc 
      (if-let [iloc (find-node-to-indent rloc)]
        (-> iloc 
          (indent-children delta) 
          (zc/move-to-node rloc))
        rloc))))

(defn insert-right
  "Insert the given sexpr to the right of the given zipper location and indent
   accordingly."
  [zloc v]
  (let [rloc (ze/insert-right zloc v)
        delta (inc (zc/length (-> rloc z/right z/right)))]
    (if (zero? delta) rloc 
      (if-let [iloc (find-node-to-indent (-> rloc z/right z/right))]
        (-> iloc 
          (indent-children delta) 
          (zc/move-to-node rloc))
        rloc))))

;; ## Remove + Indent

(defn remove
  "Remove the node at the given zipper location and indent accordingly."
  [zloc]
  (let [delta (dec (- (zc/length zloc)))
        rloc (ze/remove zloc)]
    (if (zero? delta) rloc 
      (if-let [iloc (find-node-to-indent (-> rloc z/next zc/skip-whitespace))]
        (-> iloc 
          (indent-children delta) 
          (zc/move-to-node rloc))
        rloc))))
