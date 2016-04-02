(ns rewrite-clj.custom-zipper.core-test
  (:require [clojure.test.check
              [generators :as gen]
              [properties :as prop]]
            [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [rewrite-clj.test-helpers :refer :all]
            [rewrite-clj.zip
              [base :as base]
              [whitespace :as ws]]
            [rewrite-clj.custom-zipper
             [core :as z]
             [utils :as u]]))

(fact "zipper starts with position [1 1]"
      (z/position (z/custom-zipper (node/comment-node "hello"))) => [1 1])

(tabular
  (fact "z/down tracks position correctly"
        (-> (z/custom-zipper (?type [(node/token-node "hello")]))
          z/down
          z/position) => ?pos)
  ?type            ?pos
  node/forms-node  [1 1]
  node/fn-node     [1 3]
  node/quote-node  [1 2])

(tabular
  (fact "z/right tracks position correctly"
        (let [root (base/of-string "[hello \nworld]" {:track-position? true})
              zloc (nth (iterate z/next root) ?n)]
          (z/position zloc) => ?pos))
  ?n ?pos
  1  [1 2]
  2  [1 7]
  3  [1 8]
  4  [2 1])

(fact "z/rightmost tracks position correctly"
      (let [root (base/of-string "[hello world]" {:track-position? true})]
        (-> root z/down z/rightmost z/position) => [1 8]))

(tabular
  (fact "z/left tracks position correctly"
        (let [root (base/of-string "[hello world]" {:track-position? true})
              zloc (nth (iterate z/left (z/rightmost (z/down root))) ?n)]
          (z/position zloc) => ?pos))
  ?n ?pos
  0 [1 8]
  1 [1 7]
  2 [1 2])

(tabular
  (fact "z/up tracks position correctly"
        (let [bottom (-> (base/of-string "[x [y [1]]]" {:track-position? true})
                         z/down
                         z/right z/right
                         z/down
                         z/right z/right
                         z/down)
              zloc (nth (iterate z/up bottom) ?n)]
          (z/position zloc) => ?pos))
  ?n ?pos
  0  [1 8]
  1  [1 7]
  2  [1 4]
  3  [1 1])

(fact "z/leftmost tracks position correctly"
      (-> (base/of-string "[hello world]" {:track-position? true})
        z/down
        z/right z/right
        z/leftmost
        z/position) => [1 2])

(fact "z/remove tracks position correctly"
      (let [root (base/of-string "[hello world]" {:track-position? true})]
        (-> root z/down z/remove z/position) => [1 1]
        (-> root z/down z/right z/remove z/position) => [1 2]))

(fact "z/replace doesn't change the current position"
      (-> (base/of-string "[hello world]" {:track-position? true})
        z/down
        (z/replace 'x)
        z/position) => [1 2])

(fact "z/insert-right doesn't change the current position"
      (-> (base/of-string "[hello world]" {:track-position? true})
        z/down
        (z/insert-right 'x)
        z/position) => [1 2])

(tabular
  (fact "z/insert-left fixes the position"
        (let [root (base/of-string "[hello world]" {:track-position? true})
              zloc (nth (iterate z/right (z/down root)) ?n)]
          (z/position (z/insert-left zloc 'x)) => ?pos))
  ?n ?pos
  0 [1 3]
  1 [1 8])

(def operations
  {:left                  z/left
   :right                 z/right
   :up                    z/up
   :down                  z/down
   :rightmost             z/rightmost
   :leftmost              z/leftmost
   :insert-right          #(z/insert-right % (node/newline-node "\n"))
   :insert-left           #(z/insert-left % (node/whitespace-node "  "))
   :replace               #(z/replace % (node/token-node 'RR))
   :next                  #(some-> % z/next (dissoc :end?))
   :prev                  z/prev
   :remove                z/remove
   :remove-left           u/remove-left
   :remove-right          u/remove-right
   :remove-and-move-left  u/remove-and-move-left
   :remove-and-move-right u/remove-and-move-right})

(defn apply-operations
  "Apply a sequence of operations to `zloc`, rejecting any operations which
  either throw or make `zloc` nil.  Note: we have to verify that zipping back
  up to the root doesn't fail, also."
  [zloc [op & ops]]
  (if-not op
    zloc
    (recur (or (try
                 (let [zloc' ((operations op) zloc)]
                   (z/root zloc')
                   zloc')
                 (catch Throwable t
                   nil))
               zloc)
           ops)))

(defn char-at-position
  [s [row col]]
  (loop [[c & cs] (seq s)
         [cur-row cur-col] [1 1]]
    (cond
      (= [row col] [cur-row cur-col])
      c

      (< (compare [row col] [cur-row cur-col]) 0)
      nil

      :else
      (recur cs (if (= c \newline)
                  [(inc cur-row) 1]
                  [cur-row (inc cur-col)])))))

(defn char-here
  [zloc]
  (cond
    (z/end? zloc)
    nil

    (= "" (node/string (z/node zloc)))
    (recur (z/next zloc))

    :else
    (first (node/string (z/node zloc)))))

(property "zipper position always matches row and column in root-string"
  (prop/for-all [node (g/node)
                 operations (gen/vector (gen/elements (keys operations)) 1 8)]
    (let [zloc (apply-operations
                 (base/edn* node {:track-position? true})
                 operations)]
      (= (char-here zloc)
         (char-at-position (base/root-string zloc) (z/position zloc))))))
