(ns ^:no-doc rewrite-clj.zip.walk
  (:require  [rewrite-clj.zip.move :as m]
             [rewrite-clj.zip.subedit :as subedit]))

#?(:clj (set! *warn-on-reflection* true))

(defn- downmost [zloc]
  (->> (iterate m/down zloc)
       (take-while identity)
       last))

(defn- process-loc [zloc p? f]
  (if (p? zloc)
    (or (f zloc) zloc)
    zloc))

(defn- prewalk-subtree
  [p? f zloc]
  (loop [loc zloc]
    (if (m/end? loc)
      loc
      (recur (-> loc
                 (process-loc p? f)
                 m/next)))))

(defn prewalk
  "Return zipper modified by an isolated depth-first pre-order traversal.

   Pre-order traversal visits root before children.
   For example, traversal order of `(1 (2 3 (4 5) 6 (7 8)) 9)` is:

   1. `(1 (2 3 (4 5) 6 (7 8)) 9)`
   2. `1`
   3. `(2 3 (4 5) 6 (7 8))`
   4. `2`
   5. `3`
   6. `(4 5)`
   7. `4`
   8. `5`
   9. `6`
   10. `(7 8)`
   11. `7`
   12. `8`
   13. `9`

   Traversal starts at the current node in `zloc` and continues to the end of the isolated sub-tree.

   Function `f` is called on the zipper locations satisfying predicate `p?` and must return either
   - nil to indicate no changes
   - or a valid zipper
   WARNING: when function `f` changes the location in the zipper, normal traversal will be affected.

   When `p?` is not specified `f` is called on all locations.

   Note that by default a newly created zipper automatically navigates to the first non-whitespace
   node. If you want to be sure to walk all forms in a zipper, you'll want to navigate one up prior to your walk:

   ```Clojure
   (-> (zip/of-string \"my clojure forms\")
       zip/up
       (zip/prewalk ...))
   ```

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  ([zloc f] (prewalk zloc (constantly true) f))
  ([zloc p? f]
   (->> (partial prewalk-subtree p? f)
        (subedit/subedit-node zloc))))

(defn postwalk-subtree
  [p? f zloc]
  (loop [loc (downmost zloc)]
    (let [loc (process-loc loc p? f)]
      (cond (m/right loc)
            (recur (-> loc m/right downmost))

            (m/up loc)
            (recur (m/up loc))

            :else
            loc))))

(defn postwalk
  "Return zipper modified by an isolated depth-first post-order traversal.

   Post-order traversal visits children before root.
   For example, traversal order of `(1 (2 3 (4 5) 6 (7 8)) 9)` is:

   1. `1`
   2. `2`
   3. `3`
   4. `4`
   5. `5`
   6. `(4 5)`
   7. `6`
   8. `7`
   9. `8`
   10. `(7 8)`
   11. `(2 3 (4 5) 6 (7 8))`
   12. `9`
   13. `(1 (2 3 (4 5) 6 (7 8)) 9)`

   Traversal starts at the current node in `zloc` and continues to the end of the isolated sub-tree.

   Function `f` is called on the zipper locations satisfying predicate `p?` and must return either
   - nil to indicate no changes
   - or a valid zipper
   WARNING: when function `f` changes the location in the zipper, normal traversal will be affected.

   When `p?` is not specified `f` is called on all locations.

   Note that by default a newly created zipper automatically navigates to the first non-whitespace
   node. If you want to be sure to walk all forms in a zipper, you'll want to navigate one up prior to your walk:

   ```Clojure
   (-> (zip/of-string \"my clojure forms\")
       zip/up
       (zip/postwalk ...))
   ```

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  ([zloc f] (postwalk zloc (constantly true) f))
  ([zloc p? f]
   (subedit/subedit-node zloc #(postwalk-subtree p? f %))))
