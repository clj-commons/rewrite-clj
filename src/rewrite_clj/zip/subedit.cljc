(ns ^:no-doc rewrite-clj.zip.subedit
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.options :as options])
  #?(:cljs (:require-macros [rewrite-clj.zip.subedit])) )

#?(:clj (set! *warn-on-reflection* true))

;; ## Edit Scope

(defn- path
  "Generate a seq representing a path to the current node
   starting at the root. Each element represents one `z/down`
   and the value of each element will be the number of `z/right`s
   to run."
  [zloc]
  (->> (iterate zraw/up zloc)
       (take-while zraw/up)
       (map (comp count zraw/lefts))
       (reverse)))

(defn- move-step
  "Move one down and `n` steps to the right."
  [loc n]
  (nth
    (iterate zraw/right (zraw/down loc))
    n))

(defn- move-to
  "Move to the node represented by the given path."
  [zloc path]
  (let [root (-> zloc
                 zraw/root
                 (base/of-node* (options/get-opts zloc)))]
    (reduce move-step root path)))

(defn edit-node
  "Return zipper applying function `f` to `zloc`. The resulting
   zipper will be located at the same path (i.e. the same number of
   downwards and right movements from the root) incoming `zloc`.

   See also [[subedit-node]] for an isolated edit."
  [zloc f]
  (let [zloc' (f zloc)]
    (assert (not (nil? zloc')) "function applied in 'edit-node' returned nil.")
    (move-to zloc' (path zloc))))

(defmacro edit->
  "Like `->`, threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`.

   See also [[subedit->]] for an isolated edit."
  [zloc & body]
  `(edit-node ~zloc #(-> % ~@body)))

(defmacro edit->>
  "Like `->>`, threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`.

   See also [[subedit->>]] for an isolated edit."
  [zloc & body]
  `(edit-node ~zloc #(->> % ~@body)))

;; ## Sub-Zipper

(defn subzip
  "Create and return a zipper whose root is the current node in `zloc`.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc]
  (let [zloc' (some-> zloc
                      zraw/node
                      (base/of-node* (options/get-opts zloc)))]
    (assert zloc' "could not create subzipper.")
    zloc'))

(defn subedit-node
  "Return zipper replacing current node in `zloc` with result of `f` applied to said node as an isolated sub-tree.
   The resulting zipper will be located on the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc f]
  (let [zloc' (f (subzip zloc))]
    (assert (not (nil? zloc')) "function applied in 'subedit-node' returned nil.")
    (zraw/replace zloc (zraw/root zloc'))))

(defmacro subedit->
  "Like `->`, threads `zloc`, as an isolated sub-tree through forms, then zips
   up to, and locates at, the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc & body]
  `(subedit-node ~zloc #(-> % ~@body)))

(defmacro subedit->>
  "Like `->`. Threads `zloc`, as an isolated sub-tree through forms, then zips
      up to, and locates at, the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc & body]
  `(subedit-node ~zloc #(->> % ~@body)))
