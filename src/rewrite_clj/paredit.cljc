(ns rewrite-clj.paredit
  "Paredit zipper operations for Clojure/ClojureScript/EDN.

  You might find inspiration from examples here: http://pub.gajendra.net/src/paredit-refcard.pdf"
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.custom-zipper.utils :as u]
            [rewrite-clj.node :as nd]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.findz :as fz]
            [rewrite-clj.zip.removez :as rz]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;;*****************************
;; Helpers
;;*****************************

(defn- take-upto
  ;; taken from weavejester/medley
  "Returns a lazy sequence of successive items from coll up to and including
  the first item for which `(pred item)` returns true. Returns a transducer
  when no collection is provided."
  ([pred]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result x]
        (let [result (rf result x)]
          (if (pred x)
            (ensure-reduced result)
            result))))))
  ([pred coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (let [x (first s)]
        (cons x (when-not (pred x) (take-upto pred (rest s)))))))))

;; TODO: because we skip over comments, we will consider a seq with comments as empty
;; And a seq with whitespace as empty
(defn- empty-seq? [zloc]
  (and (z/seq? zloc)
       (not (z/down zloc))
       ;; TODO: why do I need this extra test?
       (not (some-> zloc z/down z/right))))

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

(defn- nav-via [zloc fns]
  (reduce (fn [zloc f]
            (f zloc))
          zloc
          fns))

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

(defn- remove-right-sibs
  [zloc]
  (u/remove-right-while zloc (constantly true)))

;;*****************************
;; Paredit functions
;;*****************************

(defn kill
  "Returns `zloc` with the current node and all sibling nodes to the right removed.
  Locates `zloc` to node left of deleted node, else if no left node removes current node via [[rewrite-clj.zip/remove*]].

  Makes no automatic whitespace adjustments.

  - `[1 |2 3 4]  => [1| ]`
  - `[1 2 |3 4]  => [1 2| ]`
  - `[|1 2 3 4]  => |[]`
  - `[ |1 2 3 4]  => [| ]`"
  [zloc]
  (let [zloc (remove-right-sibs zloc)]
    (or (u/remove-and-move-left zloc)
        (z/remove* zloc))))

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

(defn- find-slurp-locs-up
  "Return map with
  - `:sluper-loc` - found sequence to slurp into
  - `:slurpee-loc` - found node that we will slurp
  - `:route` - vector of fns to navigate from slurper-loc to zloc"
  [zloc f]
  (let [ups (->> zloc
                 z/up
                 (iterate z/up)
                 (take-while z/up)
                 (take-while identity)
                 (take-upto f))]
    (when-let [slurpee-loc (-> ups last f)]
      (let [slurper-loc (last ups)
            route (->> ups
                      reverse
                      rest
                      (mapcat (fn [uzloc]
                                (apply conj
                                       [z/down*]
                                       (repeat (-> uzloc zraw/lefts count) z/right*))))
                      (into []))
            route (apply conj route
                         z/down* (repeat (-> zloc zraw/lefts count) z/right*))]
        {:slurper-loc slurper-loc :slurpee-loc slurpee-loc :route route}))))

(defn- find-slurp-locs [zloc f {:keys [from]}]
  ;; if slurping into an empty sequence
  (if (and (= :current from) (z/seq? zloc) (f zloc))
    {:slurper-loc zloc :slurpee-loc (f zloc) :route []}
    (some-> zloc (find-slurp-locs-up f))))

(defn ^{:added "1.1.50"} slurp-forward-into
  "Return `zloc` with node to the right of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  Optional `opts` specify:
  - `:from`
    - `:parent` (default) starts search from parent of `zloc` then upward
    - `:current` starts search from `zloc` then upward to allow slurping into empty sequences

  Generic behavior:
  - `[1 [2 |3 4] 5 6] => [1 [2 |3 4 5] 6]` Slurp into parent
  - `[[[|1 2]]] 3   => [[[|1 2]] 3]` Slurp into ancestor

  With `:from :current`:
  - `[[1 |[]]] 2 3 => [[1 |[]] 2] 3` Slurp into ancestor (same as generic behavior)
  - `[1 |[]] 2 3 => [1 |[] 2] 3` Slurp into parent
  - `[1 |[] 2] 3 => [1 |[2]] 3` Slurp into current empty sequence
  - `[1 |[2]] 3  => [1 |[2] 3]` Slurp into parent
  - `[1 |[2] 3]  => [1 |[2 3]]` Slurp into current non-empty sequence

  With `:from :parent` (default):
  - `[[1 |[]]] 2 3 => [[1 |[]] 2] 3` Slurp into ancestor (same as generic behavior)
  - `[1 |[]] 2 3 => [1 |[] 2] 3` Slurp into parent
  - `[1 |[] 2] 3 => [1 |[] 2 3]` Slurp into parent"
  ([zloc]
   (slurp-forward-into zloc {:from :parent}))
  ([zloc opts]
   (let [{:keys [slurper-loc slurpee-loc route]} (find-slurp-locs zloc z/right opts)]
     (if-not slurper-loc
       zloc
       (let [also-slurp (linebreak-and-comment-nodes slurper-loc z/right*)
             also-slurp (if (and (seq also-slurp) (nd/comment? (first also-slurp)))
                          (into [(nd/spaces 1)] also-slurp)
                          also-slurp)]
         (-> slurper-loc
             (u/remove-right-while ws/whitespace-or-comment?)  ;; remove ws lb comments before slurpee
             u/remove-right                                    ;; remove slurpee
             (reduce-into-zipper z/append-child* also-slurp)   ;; slurp in saved linebreaks and comments
             (z/append-child (z/node slurpee-loc))             ;; slurp in slurpee
             (nav-via route)))))))

(defn ^{:deprecated "1.1.49"} slurp-forward
  "DEPRECATED: we recommend [[slurp-forward-into]] instead for more control.

  Return `zloc` with node to the right of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  If there is no node to the right of parent sequence, searches upward until it finds one, if any.

  - `[1 [2 |3 4] 5 6] => [1 [2 |3 4 5] 6]`
  - `[[[|1 2 3]]] 4` => [[[|1 2 3 ]]] 4]
  - `[1 2 |[3] 4] 5 => [1 2 |[3] 4 5]`

  If located at an empty seq will ultimately slurp into that empty seq and locate to the slurped node:

  - `[1 2 |[]] 3` => `[1 2 |[] 3]`
  - `[1 2 |[] 3] => [1 2 [|3]]`"
  [zloc]
  (if (empty-seq? zloc)
    (let [zloc (slurp-forward-into zloc {:from :current})]
      (if (not (empty-seq? zloc))
        (z/down zloc)
        zloc))
    (slurp-forward-into zloc {:from :parent})))

(defn ^{:added "1.1.50"} slurp-forward-fully-into
  "Return `zloc` with all right sibling nodes of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  See also [[slurp-forward-into]].

  Optional `opts` specify:
  - `:from`
    - `:parent` (default) starts search from parent of `zloc` then upward
    - `:current` starts search from `zloc` then upward to allow slurping into empty sequences

  Generic behavior:
  - `[1 [2 |3 4] 5 6] 7 => [1 [2 |3 4 5 6]] 7` Slurp from parent
  - `[[[|1 2]] 3 4] 5   => [[[|1 2 3 5]]] 5` Slurp from ancestor

  With `:from :current`:
  - `[1 |[] 2 3] 4 5 => [1 |[2 3]] 4 5` Slurp into current empty sequence
  - `[1 |[2 3]] 4 5  => [1 |[2 3 4 5]]` Slurp into current non-empty sequence

  With `:from :parent` (default):
  - `[[1 |[]] 2 3] 4 5 => [[1 |[] 2 3] 4 5` Slurp into parent"
  ([zloc]
   (slurp-forward-fully-into zloc {:from :parent}))
  ([zloc opts]
   (let [{:keys [slurpee-loc route]} (find-slurp-locs zloc z/right opts)]
     (if (not slurpee-loc)
       zloc
       (let [n-siblings (->> slurpee-loc
                             (iterate z/right)
                             (take-while identity)
                             count)
             n-downs (->> route
                          (filter #(= z/down* %))
                          count)
             n-slurps (if (and (= :current (:from opts)) (z/seq? zloc))
                        (* (inc n-downs) n-siblings)
                        (* n-downs n-siblings))]
         (->> zloc
              (iterate #(slurp-forward-into % opts))
              (take (inc n-slurps))
              last))))))


(defn ^{:deprecated "1.1.49"} slurp-forward-fully
  "DEPRECATED: We recommend [[slurp-forward-fully-into]]] for more control.

  Return `zloc` with all right sibling nodes of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  Fully pull in first slurpable and all of its right siblings.

  - `[1 2 [|3] 4 5] 6  => [1 2 [|3 4 5]] 6`
  - `[[[|1 2]] 3 4] 5  => [[[|1 2 3 5]]] 5` Slurp from ancestor


  If located at an empty seq will ultimately slurp into that empty seq and locate to the first slurped node.

  - `[[1 2 |[]] 3 4] 5 => [[1 2 [|3 4]]] 5`"
  [zloc]
  (if (empty-seq? zloc)
    (let [zloc (slurp-forward-fully-into zloc {:from :current})]
      (if (not (empty-seq? zloc))
        (-> zloc z/down)
        zloc))
    (slurp-forward-fully-into zloc {:from :parent})))

(defn ^{:added "1.1.50"} slurp-backward-into
  "Returns `zloc` with node to the left of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  Optional `opts` specify:
  - `:from`
    - `:parent` (default) starts search from parent of `zloc` then upward
    - `:current` starts search from `zloc` then upward to allow slurping into empty sequences

  Generic behavior:
  - `[1 2 [3 |4 5] 6] => [1 [2 |3 4 5] 6]` Slurp into parent
  - `1 [[[|2 3]]]   => [1[[|2 3]]]` Slurp into ancestor

  With `:from :current`:
  - `1 2 [[|[] 3]] => 1 [2 [3 |[]]]` Slurp into ancestor (same as generic behavior)
  - `1 2 [|[] 3]   => 1 [2 |[] 3]` Slurp into parent
  - `1 [2 |[] 3]   => 1 [|[2] 3]` Slurp into current empty sequence
  - `1 [|[2] 3]    => [1 |[2] 3]` Slurp into parent
  - `[1 |[2] 3]    => [|[1 2] 3]` Slurp into current non-empty sequence

  With `:from :parent` (default):
  - `1 2 [[3 |[]]] => 1 [2 [3 |[]]]` Slurp into ancestor (same as generic behavior)
  - `1 2 [|[] 3]   => 1 [2 |[] 3]` Slurp into parent
  - `1 [2 |[] 3]   => [1 2 |[] 3]` Slurp into parent"
  ([zloc]
   (slurp-backward-into zloc {:from :parent}))
  ([zloc opts]
   (let [{:keys [slurper-loc slurpee-loc route]} (find-slurp-locs zloc z/left opts)]
     (if (not slurper-loc)
       zloc
       (let [also-slurp (linebreak-and-comment-nodes slurper-loc z/left*)
             route (if (seq route)
                     ;; compensate for inserted item, we need to skip over it
                     (apply conj [(first route)] z/right (rest route))
                     route)]
         (-> slurper-loc
             (u/remove-left-while ws/whitespace-or-comment?)
             u/remove-left
             (reduce-into-zipper z/insert-child* also-slurp)
             (z/insert-child (z/node slurpee-loc))
             (nav-via route)))))))

(defn ^{:deprecated "1.1.49"} slurp-backward
  "DEPRECATED: we recommend [[slurp-backward-into]] for more control.

  Returns `zloc` with node to the left of nearest eligible sequence slurped into that sequence else `zloc` unchanged.

  Pull in the node to right of the current node's parent sequence.
  If there is no node to the right of parent sequence, searches upward until it finds one, if any.

  - `[1 2 [3 |4 5] 6] => [1 [2 3 |4 5] 6]`
  - `1 [[[|2 3 4]]]   => [1 [[|1 2 3 ]]]`
  - `[1 2 |[3] 4] 5   => [1 2 |[3] 4 5]`

  If located at an empty seq will ultimately slurp into that empty seq and locate to the slurped node:

  - `1 [|[] 2 3]  => [1 |[] 2 3]`
  - `[1 |[] 2 3]  => [|[1] 2 3]`"
  [zloc]
  (if (empty-seq? zloc)
    (let [zloc (slurp-backward-into zloc {:from :current})]
      (if (not (empty-seq? zloc))
        (z/down zloc)
        zloc))
    (slurp-backward-into zloc {:from :parent})))

(defn ^{:added "1.1.50"} slurp-backward-fully-into
  "Returns `zloc` with all left sibling nodes of nearest eligible sequnece slurped into that sequence else `zloc`

  See also [[slurp-backward-into]].

  Optional `opts` specify:
  - `:from`
    - `:parent` (default) starts search from parent of `zloc` then upward
    - `:current` starts search from `zloc` then upward to allow slurping into empty sequences

  Generic behavior:
  - `1 [2 3 [4 |5 6] 7] => 1 [[2 3 4 |5 6] 7]` Slurp from parent
  - `1 [2 3 [[|4 5]]]   => 1 [[[2 3 |4 5]]]` Slurp from ancestor

  With `:from :current`:
  - `1 2 [3 4 |[] 5] => 1 2 [|[3 4] 5]` Slurp into current empty sequence
  - `1 2 [|[3 4] 5]  => [|[1 2 3 4] 5]` Slurp into current non-empty sequence

  With `:from :parent` (default):
  - `1 2 [[|[]] 3 4]  => [[1 2 |[] 2 3]` Slurp into parent"
  ([zloc]
   (slurp-backward-fully-into zloc {:from :parent}))
  ([zloc opts]
   (let [{:keys [slurpee-loc route]} (find-slurp-locs zloc z/left opts)]
     (if (not slurpee-loc)
       zloc
       (let [n-siblings (->> slurpee-loc
                             (iterate z/left)
                             (take-while identity)
                             count)
             n-downs (->> route
                          (filter #(= z/down* %))
                          count)
             n-slurps (if (and (= :current (:from opts)) (z/seq? zloc))
                        (* (inc n-downs) n-siblings)
                        (* n-downs n-siblings))]
          (->> zloc
              (iterate #(slurp-backward-into % opts))
              (take (inc n-slurps))
              last))))))

(defn ^{:deprecated "1.1.49"} slurp-backward-fully
  "DEPRECATED: We recommend instead [[slurp-backward-fully-into]] for more control.

  Returns `zloc` with all left sibling nodes of nearest eligible sequnece slurped into that sequence else `zloc`.

  - `1 [2 3 [|4] 5 6] => 1 [[2 3 |4] 5 6]`

  If located at an empty seq will ultimately slurp into that empty seq and locate to the first slurped node.

  - `1 [[2 3 |[]] 4 5] => 1 [[[|2 3]] 4 5]`"
  [zloc]
  (if (empty-seq? zloc)
    (let [zloc (slurp-backward-fully-into zloc {:from :current})]
      (if (not (empty-seq? zloc))
        (-> zloc z/down)
        zloc))
    (slurp-backward-fully-into zloc {:from :parent})))

(defn barf-forward
  "Returns `zloc` with rightmost node of the parent sequence pushed right out of the sequence.

  Comments and newlines preceding barfed node are also barfed.

  - `[1 2 [|3 4] 5] => [1 2 [|3] 4 5]`
  - `[1 2 [|3] 4 5] => [1 2 [] |3 4 5]`"
  [zloc]
  (if-not (z/up zloc)
    zloc
    (let [barfee-loc (z/rightmost zloc)
          also-barf (linebreak-and-comment-nodes barfee-loc z/left*)
          adjust-location (fn [zloc-barf-seq]
                            (let [left-sibs (count (zraw/lefts zloc))
                                  barf-loc (if (z/whitespace-or-comment? zloc)
                                             (or (z/right zloc) (z/left zloc))
                                             zloc)]
                              (if (= barfee-loc barf-loc)
                                (z/right zloc-barf-seq)
                                (-> zloc-barf-seq z/down (move-n z/right* left-sibs)))))
          adjust-ws (fn [zloc-before-also-barf]
                      (if (and (seq also-barf)
                               (some-> zloc-before-also-barf z/right* z/whitespace?))
                        (u/remove-right zloc-before-also-barf)
                        zloc-before-also-barf))]
      (-> barfee-loc
          (u/remove-left-while ws/whitespace-or-comment?)
          (u/remove-right-while ws/whitespace?)
          u/remove-and-move-up
          (z/insert-right (z/node barfee-loc))
          adjust-ws
          (reduce-into-zipper z/insert-right* also-barf)
          adjust-location))))

(defn barf-backward
  "Returns `zloc` with leftmost node of the parent sequence pushed left out of the sequence.

  - `[1 2 [3 |4] 5] => [1 2 3 [|4] 5]`
  - `[1 2 3 [|4] 5] => [1 2 3 |4 [] 5]`"
  [zloc]
  (if-not (z/up zloc)
    zloc
    (let [barfee-loc (z/leftmost zloc)
          also-barf (linebreak-and-comment-nodes barfee-loc z/right*)
          adjust-location (fn [zloc-barf-seq]
                            (let [right-sibs (count (zraw/rights zloc))
                                  barf-loc (if (z/whitespace-or-comment? zloc)
                                             (or (z/left zloc) (z/right zloc))
                                             zloc)]

                              (if (= barfee-loc barf-loc)
                                (z/left zloc-barf-seq)
                                (-> zloc-barf-seq z/down* z/rightmost* (move-n z/left* right-sibs)))))]
      (-> barfee-loc
          (u/remove-left-while ws/whitespace?)
          (u/remove-right-while ws/whitespace-or-comment?)
          u/remove-and-move-up
          (z/insert-left (z/node barfee-loc))
          (reduce-into-zipper z/insert-left* also-barf)
          adjust-location))))

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
      (slurp-forward-fully-into {:from :current})
      z/down))

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
