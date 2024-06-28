(ns rewrite-clj.custom-zipper.core-test
  (:require [clojure.test :refer [deftest is]]
            #?(:cljs [clojure.test.check :refer-macros [quick-check]])
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop #?@(:cljs [:include-macros true])]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [rewrite-clj.zip.base :as base]))

(deftest t-zipper-starts-with-position-1-1
  (is (= [1 1] (z/position (z/custom-zipper (node/comment-node "hello"))))))

(deftest t-zdown-tracks-position-correctly
  (doseq [[create-fn pos]
          [[node/forms-node  [1 1]]
           [node/fn-node     [1 3]]
           [node/quote-node  [1 2]]]]
    (is (= pos
           (-> (z/custom-zipper (create-fn [(node/token-node "hello")]))
               z/down
               z/position)))))

(deftest t-zright-tracks-position-correctly
  (doseq [[n pos]
          [[1  [1 2]]
           [2  [1 7]]
           [3  [1 8]]
           [4  [2 1]]]]
    (let [root (base/of-string "[hello \nworld]" {:track-position? true})
          zloc (nth (iterate z/next root) n)]
      (is (= pos (z/position zloc))))))

(deftest t-zrightmost-tracks-position-correctly
  (let [root (base/of-string "[hello world]" {:track-position? true})]
    (is (= [1 8] (-> root z/down z/rightmost z/position)))))

(deftest t-zleft-tracks-position-correctly
  (doseq [[n pos]
          [[0 [1 8]]
           [1 [1 7]]
           [2 [1 2]]]]
    (let [root (base/of-string "[hello world]" {:track-position? true})
          zloc (nth (iterate z/left (z/rightmost (z/down root))) n)]
      (is (= pos (z/position zloc))))))

(deftest t-zup-tracks-position-correctly
  (doseq [[n pos]
          [[0  [1 8]]
           [1  [1 7]]
           [2  [1 4]]
           [3  [1 1]]]]
    (let [bottom (-> (base/of-string "[x [y [1]]]" {:track-position? true})
                     z/down
                     z/right z/right
                     z/down
                     z/right z/right
                     z/down)
          zloc (nth (iterate z/up bottom) n)]
      (is (= pos (z/position zloc))))))

(deftest t-zleftmost-tracks-position-correctly
  (is (= [1 2]
         (-> (base/of-string "[hello world]" {:track-position? true})
             z/down
             z/right z/right
             z/leftmost
             z/position))))

(deftest t-zremove-tracks-position-correctly
  (let [root (base/of-string "[hello world]" {:track-position? true})]
    (is (= [1 1] (-> root z/down z/remove z/position)))
    (is (= [1 2] (-> root z/down z/right z/remove z/position)))))

(deftest t-zreplace-doesnt-change-the-current-position
  (is (= [1 2]
         (-> (base/of-string "[hello world]" {:track-position? true})
             z/down
             (z/replace 'x)
             z/position))))

(deftest t-zinsert-right-doesnt-change-the-current-position
  (is (= [1 2]
         (-> (base/of-string "[hello world]" {:track-position? true})
             z/down
             (z/insert-right 'x)
             z/position))))

(deftest t-zinsert-left-fixes-the-position
  (doseq [[n pos]
          [[0 [1 3]]
           [1 [1 8]]]]
    (let [root (base/of-string "[hello world]" {:track-position? true})
          zloc (nth (iterate z/right (z/down root)) n)]
      (is (= pos (z/position (z/insert-left zloc 'x)))))))

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
                 (catch #?(:clj Throwable
                           :cljs :default) _t
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

(defspec t-zipper-position-always-matches-row-and-column-in-root-string
  (prop/for-all [node (g/node)
                 operations (gen/vector (gen/elements (keys operations)) 1 8)]
                (let [zloc (apply-operations
                            (base/of-node* node {:track-position? true})
                            operations)]
                  (= (char-here zloc)
                     (char-at-position (base/root-string zloc) (z/position zloc))))))
