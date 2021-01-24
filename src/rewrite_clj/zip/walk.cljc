(ns ^:no-doc rewrite-clj.zip.walk
  (:require [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.subedit :refer [subedit-node]]))

#?(:clj (set! *warn-on-reflection* true))

(defn- prewalk-subtree
  [p? f zloc]
  (loop [loc zloc]
    (if (m/end? loc)
      loc
      (if (p? loc)
        (if-let [n (f loc)]
          (recur (m/next n))
          (recur (m/next loc)))
        (recur (m/next loc))))))

(defn prewalk
  "Return zipper modified by an isolated depth-first pre-order traversal.
   Traversal starts at the current node in `zloc` and continues to the end of the isolated sub-tree.
   Function `f` is called on the zipper locations satisfying predicate `p?` and must return a valid zipper - modified or not.

   When `p?` is not specified `f` is called all locations.

   Note that by default a newly created zipper automatically navigates to the first non-whitespace
   node. If you want to be sure to walk all forms in a zipper, you'll want to navigate one up prior to your walk:

   ```Clojure
   (-> (zip/of-string \"my clojure forms\")
       zip/up
       (zip/prewalk ...))
   ```

   WARNING: when function `f` changes the location in the zipper, normal traversal will be affected."
  ([zloc f] (prewalk zloc (constantly true) f))
  ([zloc p? f]
   (->> (partial prewalk-subtree p? f)
        (subedit-node zloc))))

(defn postwalk-subtree
  [p? f loc]
  (let [nloc (m/next loc)
        loc' (if (m/end? nloc)
               loc
               (m/prev (postwalk-subtree p? f nloc)))]
    (if (p? loc')
      (or (f loc') loc')
      loc')))

(defn ^{:added "0.4.9"} postwalk
  "Return zipper modified by an isolated depth-first post-order traversal.
   Traversal starts at the current node in `zloc` and continues to the end of the isolated sub-tree.
   Function `f` is called on the zipper locations satisfying predicate `p?`, and must return a valid zipper - modified or not.

   When `p?` is not specified `f` is called all locations.

   Note that by default a newly created zipper automatically navigates to the first non-whitespace
   node. If you want to be sure to walk all forms in a zipper, you'll want to navigate one up prior to your walk:

   ```Clojure
   (-> (zip/of-string \"my clojure forms\")
       zip/up
       (zip/postwalk ...))
   ```

   WARNING: when function `f` changes the location in the zipper, normal traversal will be affected."
  ([zloc f] (postwalk zloc (constantly true) f))
  ([zloc p? f]
   (subedit-node zloc #(postwalk-subtree p? f %))))
