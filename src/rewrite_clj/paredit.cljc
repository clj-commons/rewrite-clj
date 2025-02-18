(ns rewrite-clj.paredit
  "Paredit zipper operations for Clojure/ClojureScript/EDN.

  You might find inspiration from examples here: http://pub.gajendra.net/src/paredit-refcard.pdf"
  (:require [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node :as nd]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.findz :as fz]
            [rewrite-clj.zip.removez :as rz]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;;*****************************
;; Helpers
;;*****************************

(defn- empty-seq? [zloc]
  (and (z/seq? zloc) (not (seq (z/sexpr zloc)))))

(defn- move-n [loc f n]
  (if (= 0 n)
    loc
    (->> loc (iterate f) (take (inc n)) last)))

(defn- top
  [zloc]
  (->> zloc
       (iterate z/up)
       (take-while identity)
       last))

(defn- global-find-by-node
  [zloc n]
  (-> zloc
      top
      (z/find z/next* #(= (meta (z/node %)) (meta n)))))

(defn- nodes-by-dir
  ([zloc f] (nodes-by-dir zloc f constantly))
  ([zloc f p?]
   (->> zloc
        (iterate f)
        (take-while identity)
        (take-while p?)
        (map z/node))))

(defn- reduce-into-zipper
  "A thread-first-friendly reducer"
  [zloc f items]
  (reduce f zloc items))

(defn- linebreak-and-comment-nodes
  "Return vector of all linebreak and comment nodes from whitespace and comment nodes from `zloc` moving via `f` "
  [zloc f]
  (->> (-> zloc
           f
           (nodes-by-dir f ws/whitespace-or-comment?))
       (filterv #(or (nd/linebreak? %) (nd/comment? %)))))

(defn- remove-first-if-ws [nodes]
  (when (seq nodes)
    (if (nd/whitespace? (first nodes))
      (rest nodes)
      nodes)))

(defn- create-seq-node
  "Creates a sequence node of given type `t` with node values of `v`"
  [t v]
  (case t
    :list (nd/list-node v)
    :vector (nd/vector-node v)
    :map (nd/map-node v)
    :set (nd/set-node v)
    (throw (ex-info (str "usupported wrap type: " t) {}))))

(defn- string-node? [zloc]
  (= (some-> zloc z/node type) (type (nd/string-node " "))))

;;*****************************
;; Paredit functions
;;*****************************

(defn kill
  "Kill all sibling nodes to the right of the current node in `zloc`.

  - `[1 2| 3 4] => [1 2|]`"
  [zloc]
  (let [left (z/left* zloc)]
    (-> zloc
        (u/remove-right-while (constantly true))
        z/remove*
        (#(if left
            (global-find-by-node % (z/node left))
            %)))))

(defn- kill-in-string-node [zloc pos]
  (if (= (z/string zloc) "\"\"")
    (z/remove zloc)
    (let [bounds (-> zloc z/node meta)
          row-idx (- (:row pos) (:row bounds))
          sub-length (if-not (= (:row pos) (:row bounds))
                       (dec (:col pos))
                       (- (:col pos) (inc (:col bounds))))]

      (-> (take (inc row-idx) (-> zloc z/node :lines))
          vec
          (update-in [row-idx] #(subs % 0 sub-length))
          (#(z/replace zloc (nd/string-node %)))))))

(defn- kill-in-comment-node [zloc pos]
  (let [col-bounds (-> zloc z/node meta :col)]
    (if (= (:col pos) col-bounds)
      (z/remove zloc)
      (-> zloc
          (z/replace (-> zloc
                         z/node
                         :s
                         (subs 0 (- (:col pos) col-bounds 1))
                         nd/comment-node))
          (#(if (z/right* %)
              (z/insert-right* % (nd/newlines 1))
              %))))))

(defn kill-at-pos
  "In string and comment aware kill

  Perform kill for given position `pos` Like [[kill]], but:

  - if inside string kills to end of string and stops there
  - If inside comment kills to end of line (not including linebreak)

  - `zloc` location is (inclusive) starting point for `pos` depth-first search
  - `pos` can be a `{:row :col}` map or a `[row col]` vector. The `row` and `col` values are
  1-based and relative to the start of the source code the zipper represents.

  Throws if `zloc` was not created with [position tracking](/doc/01-user-guide.adoc#position-tracking)."
  [zloc pos]
  (if-let [candidate (z/find-last-by-pos zloc pos)]
    (let [pos (fz/pos-as-map pos)]
      (cond
        (string-node? candidate)                             (kill-in-string-node candidate pos)
        (ws/comment? candidate)                              (kill-in-comment-node candidate pos)
        (and (empty-seq? candidate)
             (> (:col pos) (-> candidate z/node meta :col))) (z/remove candidate)
        :else                                                (kill candidate)))
    zloc))

(defn- find-word-bounds
  "Return `[start-col end-col]` of word spanning 1-based `col` in `s`.
  Else nil if `col` is not in a word."
  [s col]
  (when (and (> col 0)
             (<= col (count s))
             (not (#{\space \newline} (nth s (dec col)))))
    [(->> s
          (take col)
          reverse
          (take-while #(not (= % \space)))
          count
          (- col)
          inc)
     (->> s
          (drop col)
          (take-while #(not (or (= % \space) (= % \newline))))
          count
          (+  col))]))

(defn- remove-word-at
  "Return `s` with word at 1-based `col` removed.
  If no word at `col` returns `s` unchanged"
  [s col]
  (if-let [[start end] (find-word-bounds s col)]
    (str (subs s 0 (dec start))
         (subs s end))
    s))

(defn- kill-word-in-comment-node [zloc pos]
  (let [col-bounds (-> zloc z/position fz/pos-as-map :col)]
    (-> zloc
        (z/replace (-> zloc
                       z/node
                       :s
                       (remove-word-at (- (:col pos) col-bounds))
                       nd/comment-node)))))

(defn- kill-word-in-string-node [zloc pos]
  (let [bounds (-> zloc z/position fz/pos-as-map)
        row-idx (- (:row pos) (:row bounds))
        col (if (= 0 row-idx)
              (- (:col pos) (:col bounds))
              (:col pos))]
    (-> zloc
        (z/replace (-> zloc
                       z/node
                       :lines
                       (update-in [row-idx]
                                  #(remove-word-at % col))
                       nd/string-node)))))

(defn kill-one-at-pos
  "Return `zloc` with node/word found at `pos` removed.

  If `pos` is:
  - inside a string or comment, removes word at `pos`, if at whitespace, no-op.
  - otherwise removes node and moves left, or if no left node removes via [[rewrite-clj.zip/remove]].
  If `pos` locates to whitespace between nodes, skips right to find node.

  `zloc` location is (exclusive) starting point for `pos` search
  `pos` can be a `{:row :col}` map or a `[row col]` vector. The `row` and `col` values are
  1-based and relative to the start of the source code the zipper represents.

  Throws if `zloc` was not created with [position tracking](/doc/01-user-guide.adoc#position-tracking).

  - `(+ |100 200)     => (|+ 200)`
  - `(foo |(bar do))  => (foo)`
  - `[|10 20 30]`     => |[20 30]`
  - `\"|hello world\" => \"| world\"`
  - `; |hello world   => ;  |world`"
  [zloc pos]
  (if-let [candidate (->> (z/find-last-by-pos zloc pos)
                          (ws/skip z/right* ws/whitespace?))]
    (let [pos (fz/pos-as-map pos)
          candidate-pos (-> candidate z/position fz/pos-as-map)
          kill-in-node? (not (and (= (:row pos) (:row candidate-pos))
                                  (<= (:col pos) (:col candidate-pos))))]
      (cond
        (and kill-in-node? (string-node? candidate)) (kill-word-in-string-node candidate pos)
        (and kill-in-node? (ws/comment? candidate)) (kill-word-in-comment-node candidate pos)
        :else
        (or (rz/remove-and-move-left candidate)
            (z/remove candidate))))
    zloc))

(defn- find-slurpee-up [zloc f]
  (loop [l (z/up zloc)
         n 1]
    (cond
      (nil? l) nil
      (not (nil? (f l))) [n (f l)]
      (nil? (z/up l)) nil
      :else (recur (z/up l) (inc n)))))

(defn- find-slurpee [zloc f]
  (if (empty-seq? zloc)
    [(f zloc) 0]
    (some-> zloc (find-slurpee-up f) reverse)))

(defn slurp-forward
  "Pull in next right outer node (if none at first level, tries next etc) into
  current S-expression

  - `[1 2 [|3] 4 5] => [1 2 [|3 4] 5]`"
  [zloc]
  (let [[slurpee-loc n-ups] (find-slurpee zloc z/right)]
    (if-not slurpee-loc
      zloc
      (let [slurper-loc (move-n zloc z/up n-ups)
            preserves (->> (-> slurper-loc
                               z/right*
                               (nodes-by-dir z/right* #(not (= (z/node slurpee-loc) (z/node %)))))
                           (filter #(or (nd/linebreak? %) (nd/comment? %))))]
        (-> slurper-loc
            (u/remove-right-while ws/whitespace-or-comment?)
            u/remove-right
            ((partial reduce z/append-child) preserves)
            (z/append-child (z/node slurpee-loc))
            (#(if (empty-seq? zloc)
                (-> % z/down (u/remove-left-while ws/whitespace?))
                (global-find-by-node % (z/node zloc)))))))))

(defn slurp-forward-fully
  "Pull in all right outer-nodes into current S-expression, but only the ones at the same level
  as the the first one.

  - `[1 2 [|3] 4 5] => [1 2 [|3 4 5]]`"
  [zloc]
  (let [curr-slurpee (some-> zloc (find-slurpee z/right) first)
        num-slurps (some-> curr-slurpee (nodes-by-dir z/right) count inc)]

    (->> zloc
         (iterate slurp-forward)
         (take num-slurps)
         last)))

(defn slurp-backward
  "Pull in prev left outer node (if none at first level, tries next etc) into
  current S-expression

  - `[1 2 [|3] 4 5] => [1 [2 |3] 4 5]`"
  [zloc]
  (if-let [[slurpee-loc _] (find-slurpee zloc z/left)]
    (let [preserves (->> (-> slurpee-loc
                             z/right*
                             (nodes-by-dir z/right* ws/whitespace-or-comment?))
                         (filter #(or (nd/linebreak? %) (nd/comment? %))))]
      (-> slurpee-loc
          (u/remove-left-while ws/whitespace-not-linebreak?)
          (#(if (and (z/left slurpee-loc)
                     (not (ws/linebreak? (z/left* %))))
              (ws/insert-space-left %)
              %))
          (u/remove-right-while ws/whitespace-or-comment?)
          z/remove*
          z/next
          ((partial reduce z/insert-child) preserves)
          (z/insert-child (z/node slurpee-loc))
          (#(if (empty-seq? zloc)
              (-> % z/down (u/remove-right-while ws/linebreak?))
              (global-find-by-node % (z/node zloc))))))
    zloc))

(defn slurp-backward-fully
  "Pull in all left outer-nodes into current S-expression, but only the ones at the same level
  as the the first one.

  - `[1 2 [|3] 4 5] => [[1 2 |3] 4 5]`"
  [zloc]
  (let [curr-slurpee (some-> zloc (find-slurpee z/left) first)
        num-slurps (some-> curr-slurpee (nodes-by-dir z/left) count inc)]

    (->> zloc
         (iterate slurp-backward)
         (take num-slurps)
         last)))

(defn barf-forward
  "Push out the rightmost node of the current S-expression into outer right form.

  - `[1 2 [|3 4] 5] => [1 2 [|3] 4 5]`"
  [zloc]
  (let [barfee-loc (z/rightmost zloc)]

    (if-not (z/up zloc)
      zloc
      (let [preserves (->> (-> barfee-loc
                               z/left*
                               (nodes-by-dir z/left* ws/whitespace-or-comment?))
                           (filter #(or (nd/linebreak? %) (nd/comment? %)))
                           reverse)]
        (-> barfee-loc
            (u/remove-left-while ws/whitespace-or-comment?)
            (u/remove-right-while ws/whitespace?)
            u/remove-and-move-up
            (z/insert-right (z/node barfee-loc))
            ((partial reduce z/insert-right) preserves)
            (#(or (global-find-by-node % (z/node zloc))
                  (global-find-by-node % (z/node barfee-loc)))))))))

(defn barf-backward
  "Push out the leftmost node of the current S-expression into outer left form.

  - `[1 2 [3 |4] 5] => [1 2 3 [|4] 5]`"
  [zloc]
  (let [barfee-loc (z/leftmost zloc)]
    (if-not (z/up zloc)
      zloc
      (let [preserves (->> (-> barfee-loc
                               z/right*
                               (nodes-by-dir z/right* ws/whitespace-or-comment?))
                           (filter #(or (nd/linebreak? %) (nd/comment? %))))]
        (-> barfee-loc
            (u/remove-left-while ws/whitespace?)
            (u/remove-right-while ws/whitespace-or-comment?) ;; probably insert space when on same line !
            z/remove*
            (z/insert-left (z/node barfee-loc))
            ((partial reduce z/insert-left) preserves)
            (#(or (global-find-by-node % (z/node zloc))
                  (global-find-by-node % (z/node barfee-loc)))))))))

(defn wrap-around
  "Wrap current node with a given type `t` where `t` can be one of `:vector`, `:list`, `:set`, `:map` `:fn`.

  - `|123 => [|123]` given `:vector`
  - `|[1 [2]] => [|[1 [2]]]`"
  [zloc t]
  (-> zloc
      (z/insert-left (create-seq-node t nil))
      z/left
      (u/remove-right-while ws/whitespace?)
      u/remove-right
      (z/append-child* (z/node zloc))
      z/down))

(defn wrap-fully-forward-slurp
  "Create a new seq node of type `t` left of `zloc` then slurp fully into the new node

  - `[1 |2 3 4] => [1 [|2 3 4]]`"
  [zloc t]
  (-> zloc
      (z/insert-left (create-seq-node t nil))
      z/left
      slurp-forward-fully))

(def splice
  "See [[rewrite-clj.zip/splice]]"
  z/splice)

(defn- splice-killing
  [zloc f]
  (if-not (z/up zloc)
    zloc
    (-> zloc
        (f (constantly true))
        z/up
        splice
        (global-find-by-node (z/node zloc)))))

(defn splice-killing-backward
  "Remove left siblings of current given node in S-Expression and unwrap remaining into enclosing S-expression

  - `(foo (let ((x 5)) |(sqrt n)) bar) => (foo (sqrt n) bar)`"
  [zloc]
  (splice-killing zloc u/remove-left-while))

(defn splice-killing-forward
  "Remove current given node and its right siblings in S-Expression and unwrap remaining into enclosing S-expression

  - `(a (b c |d e) f) => (a b |c f)`"
  [zloc]
  (if (and (z/up zloc) (not (z/leftmost? zloc)))
    (splice-killing (z/left zloc) u/remove-right-while)
    (if (z/up zloc)
      (-> zloc z/up z/remove)
      zloc)))

(defn split
  "Split current s-sexpression in two at given node `zloc`

  -  `[1 2 |3 4 5] => [1 2 3] [4 5]`"
  [zloc]
  (let [parent-loc (z/up zloc)]
    (if-not parent-loc
      zloc
      (let [t (z/tag parent-loc)
            lefts (reverse (remove-first-if-ws (rest (nodes-by-dir (z/right zloc) z/left*))))
            rights (remove-first-if-ws (nodes-by-dir (z/right zloc) z/right*))]

        (if-not (and (seq lefts) (seq rights))
          zloc
          (-> parent-loc
              (z/insert-left (create-seq-node t lefts))
              (z/insert-left (create-seq-node t rights))
              z/remove
              (#(or (global-find-by-node % (z/node zloc))
                    (global-find-by-node % (last lefts))))))))))

(defn- split-string [zloc pos]
  (let [bounds (-> zloc z/node meta)
        row-idx (- (:row pos) (:row bounds))
        lines (-> zloc z/node :lines)
        split-col (if-not (= (:row pos) (:row bounds))
                    (dec (:col pos))
                    (- (:col pos) (inc (:col bounds))))]
    (-> zloc
        (z/replace (nd/string-node
                    (-> (take (inc row-idx) lines)
                        vec
                        (update-in [row-idx] #(subs % 0 split-col)))))
        (z/insert-right (nd/string-node
                         (-> (drop row-idx lines)
                             vec
                             (update-in [0] #(subs % split-col))))))))

(defn split-at-pos
  "In-string aware split. Returns `zloc` with node found at `pos` split.
  If `pos` is inside a string, splits string into two strings else calls [[split]].

  - `zloc` location is (inclusive) starting point for `pos` depth-first search
  - `pos` can be a `{:row :col}` map or a `[row col]` vector. The `row` and `col` values are
  1-based and relative to the start of the source code the zipper represents.

  Throws if `zloc` was not created with [position tracking](/doc/01-user-guide.adoc#position-tracking).

  - `[1 2 |3 4 5]       => [1 2 |3] [4 5]`
  - `(\"Hello |World\") => (|\"Hello\" \"World\")`"
  [zloc pos]
  (if-let [candidate (z/find-last-by-pos zloc pos)]
    (let [pos (fz/pos-as-map pos)
          candidate-pos (fz/pos-as-map (-> candidate z/position fz/pos-as-map))]
      (if (and (string-node? candidate) (not= pos candidate-pos))
        (split-string candidate pos)
        (split candidate)))
    zloc))

(defn- join-seqs [left right]
  (let [rights (-> right z/node nd/children)
        ws-nodes (-> (z/right* left) (nodes-by-dir z/right* ws/whitespace-or-comment?))
        ws-nodes (if (seq ws-nodes)
                   ws-nodes
                   [(nd/spaces 1)])
        zloc (-> left
                 (reduce-into-zipper z/append-child* ws-nodes)
                 (reduce-into-zipper z/append-child* rights))]
    (-> zloc
        (u/remove-right-while ws/whitespace-or-comment?)
        z/right*
        u/remove-and-move-left
        z/down
        z/rightmost*
        (move-n z/left* (dec (count rights))))))

(defn- join-strings [left right]
  (let [cmts-and-nls (linebreak-and-comment-nodes left z/right*)
        cmts-and-nls (when (seq cmts-and-nls)
                       (into [(nd/spaces 1)] cmts-and-nls))]
    (-> right
        z/remove*
        z/left
        (u/remove-right-while ws/whitespace-or-comment?)
        ;; sexpr is safe on strings
        (z/replace (nd/string-node (str (-> left z/node nd/sexpr)
                                        (-> right z/node nd/sexpr))))
        (reduce-into-zipper z/insert-right* (reverse cmts-and-nls)))))

(defn join
  "Returns `zloc` with sequence to the left joined to sequence to the right.
  Also works for strings.
  If sequence types differ, uses sequence type to the left.

  - `[1 2] |[3 4]            => [1 2 |3 4]`
  - `[1 2]| [3 4]            => [1 2 |3 4]`
  - `{:a 1} |(:b 2)          => `{:a 1 :b 2}`
  - `[\"Hello\" | \"World\"] => [|\"HelloWorld\"]`"
  [zloc]
  (let [left (some-> zloc z/left)
        right (if (some-> zloc z/node nd/whitespace?) (z/right zloc) zloc)]

    (if-not (and left right)
      zloc
      (cond
        (and (z/seq? left) (z/seq? right)) (join-seqs left right)
        (and (string-node? left) (string-node? right)) (join-strings left right)
        :else zloc))))

(defn raise
  "Delete siblings and raise node at zloc one level up

  - `[1 [2 |3 4]] => [1 |3]`"
  [zloc]
  (if-let [containing (z/up zloc)]
    (-> containing
        (z/replace (z/node zloc)))
    zloc))

(defn move-to-prev
  "Move node at current location to the position of previous location given a depth first traversal

    -  `(+ 1 (+ 2 |3) 4) => (+ 1 (+ |3 2) 4)`
    - `(+ 1 (+ 2 3) |4) => (+ 1 (+ 2 3 |4))`

  returns zloc after move or given zloc if a move isn't possible"
  [zloc]
  (let [n (z/node zloc)
        p (some-> zloc z/left z/node)
        ins-fn (if (or (nil? p) (= (-> zloc z/remove z/node) p))
                 #(-> % (z/insert-left n) z/left)
                 #(-> % (z/insert-right n) z/right))]
    (if-not (-> zloc z/remove z/prev)
      zloc
      (-> zloc
          z/remove
          ins-fn))))
