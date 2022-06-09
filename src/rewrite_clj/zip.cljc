;; DO NOT EDIT FILE, automatically generated from: ./template/rewrite_clj/zip.cljc
(ns rewrite-clj.zip
  "A rich API for navigating and updating Clojure/ClojureScripti/EDN source code via a zipper tree.

  The zipper holds a tree of nodes representing source code. It also holds your current location while navigating
  through the tree and any pending changes you have made. Changes are applied back into the tree
  when invoking root functions.

  Although they are preserved, whitespace and comment nodes are normally skipped when navigating through the tree.
  There are times when you will want to include whitespace and comment nodes, and as you see below, provisions are
  available to do so.

  It is good to remember that while some function names convey mutation, we are never changing anything, we are
  instead returning modified copies.

  Some conventions in the code and docstrings:
  - `zloc` is the used as the argument name for our zipper
  - \"current node in `zloc`\" is shorthand for: node at current location in zipper `zloc`

  Because this API contains many functions, we offer the following categorized listing:

  **Create a zipper**
  [[of-node]]
  [[of-node*]]
  [[of-string]]
  [[of-file]]

  **Move**
  [[left]]
  [[right]]
  [[up]]
  [[down]]
  [[prev]]
  [[next]]
  [[leftmost]]
  [[rightmost]]

  **Move without skipping whitespace and comments**
  [[left*]]
  [[right*]]
  [[up*]]
  [[down*]]
  [[prev*]]
  [[next*]]
  [[leftmost*]]
  [[rightmost*]]

  **Whitespace/comment aware skip**
  [[skip]]
  [[skip-whitespace]]
  [[skip-whitespace-left]]

  **Test for whitespace**
  [[whitespace?]]
  [[linebreak?]]
  [[whitespace-or-comment?]]

  **Test location**
  [[leftmost?]]
  [[rightmost?]]
  [[end?]]

  **Test data type**
  [[seq?]]
  [[list?]]
  [[vector?]]
  [[set?]]
  [[map?]]
  [[namespaced-map?]]

  **Find**
  [[find]]
  [[find-next]]
  [[find-depth-first]]
  [[find-next-depth-first]]
  [[find-tag]]
  [[find-next-tag]]
  [[find-value]]
  [[find-next-value]]
  [[find-token]]
  [[find-next-token]]
  [[find-last-by-pos]]
  [[find-tag-by-pos]]

  **Inspect**
  [[node]]
  [[position]]
  [[position-span]]
  [[tag]]
  [[length]]

  **Convert**
  [[sexpr-able?]]
  [[sexpr]]
  [[child-sexprs]]
  [[reapply-context]]

  **Update**
  [[replace]]
  [[edit]]
  [[splice]]
  [[prefix]]
  [[suffix]]
  [[insert-right]]
  [[insert-left]]
  [[insert-child]]
  [[insert-space-left]]
  [[insert-space-right]]
  [[insert-newline-left]]
  [[insert-newline-right]]
  [[append-child]]
  [[remove]]
  [[remove-preserve-newline]]
  [[root]]

  **Update without coercion**
  [[replace*]]
  [[edit*]]

  **Update without whitespace treatment**
  [[insert-left*]]
  [[insert-right*]]
  [[insert-child*]]
  [[append-child*]]
  [[remove*]]

  **Update without changing location**
  [[edit-node]]
  [[edit->]]
  [[edit->>]]

  **Isolated update without changing location**
  [[subedit-node]]
  [[subzip]]
  [[prewalk]]
  [[postwalk]]
  [[subedit->]]
  [[subedit->>]]

  **Sequence operations**
  [[map]]
  [[map-keys]]
  [[map-vals]]
  [[get]]
  [[assoc]]

  **Stringify**
  [[string]]
  [[root-string]]

  **Output**
  [[print]]
  [[print-root]]"
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.custom-zipper.core]
            [rewrite-clj.node.coercer] ;; load coercions to make them available
            [rewrite-clj.zip.base]
            [rewrite-clj.zip.context]
            [rewrite-clj.zip.editz]
            [rewrite-clj.zip.findz]
            [rewrite-clj.zip.insert]
            [rewrite-clj.zip.move]
            [rewrite-clj.zip.removez]
            [rewrite-clj.zip.seqz]
            [rewrite-clj.zip.subedit #?@(:cljs [:include-macros true])]
            [rewrite-clj.zip.walk]
            [rewrite-clj.zip.whitespace])
  #?(:cljs (:require-macros [rewrite-clj.zip])))

#?(:clj (set! *warn-on-reflection* true))


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn node
  "Returns the current node in `zloc`."
  [zloc] (rewrite-clj.custom-zipper.core/node zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn position
  "Returns the ones-based `[row col]` of the start of the current node in `zloc`.

  Throws if `zloc` was not created with [position tracking](/doc/01-user-guide.adoc#position-tracking)."
  [zloc] (rewrite-clj.custom-zipper.core/position zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn position-span
  "Returns the ones-based `[[start-row start-col] [end-row end-col]]` of the current node in `zloc`.
  `end-col` is exclusive.

  Throws if `zloc` was not created with [position tracking](/doc/01-user-guide.adoc#position-tracking)."
  [zloc] (rewrite-clj.custom-zipper.core/position-span zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn root
  "Zips all the way up `zloc` and returns the root node, reflecting any changes."
  [zloc] (rewrite-clj.custom-zipper.core/root zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:added "0.4.4"} child-sexprs
  "Return s-expression (the Clojure forms) of children of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  [zloc] (rewrite-clj.zip.base/child-sexprs zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:added "1.1.45"} of-node*
  "Create and return zipper from a rewrite-clj `node` (likely parsed by [[rewrite-clj.parser]]).

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (rewrite-clj.zip.base/of-node* node))
  ([node opts] (rewrite-clj.zip.base/of-node* node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:deprecated "1.1.45"} edn*
  "DEPRECATED. Renamed to [[of-node*]]."
  ([node] (rewrite-clj.zip.base/edn* node))
  ([node opts] (rewrite-clj.zip.base/edn* node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:added "1.1.45"} of-node
  "Create and return zipper from a rewrite-clj `node` (likely parsed by [[rewrite-clj.parser]]),
  and move to the first non-whitespace/non-comment child. If node is not forms node, is wrapped in forms node
  for a consistent root.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (rewrite-clj.zip.base/of-node node))
  ([node opts] (rewrite-clj.zip.base/of-node node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:deprecated "1.1.45"} edn
  "DEPRECATED. Renamed to [[of-node]]."
  ([node] (rewrite-clj.zip.base/edn node))
  ([node opts] (rewrite-clj.zip.base/edn node opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn tag
  "Return tag of current node in `zloc`."
  [zloc] (rewrite-clj.zip.base/tag zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn sexpr
  "Return s-expression (the Clojure form) of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  [zloc] (rewrite-clj.zip.base/sexpr zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn sexpr-able?
  "Return true if current node's element type in `zloc` can be [[sexpr]]-ed.

   See [related docs in user guide](/doc/01-user-guide.adoc#not-all-clojure-is-sexpr-able)"
  [zloc] (rewrite-clj.zip.base/sexpr-able? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn length
  "Return length of printable [[string]] of current node in `zloc`."
  [zloc] (rewrite-clj.zip.base/length zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED. Return a tag/s-expression pair for inner nodes, or
   the s-expression itself for leaves."
  [zloc] (rewrite-clj.zip.base/value zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn of-string
  "Create and return zipper from all forms in Clojure/ClojureScript/EDN string `s`.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([s] (rewrite-clj.zip.base/of-string s))
  ([s opts] (rewrite-clj.zip.base/of-string s opts)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:added "0.4.0"} string
  "Return string representing the current node in `zloc`."
  [zloc] (rewrite-clj.zip.base/string zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Renamed to [[string]]."
  [zloc] (rewrite-clj.zip.base/->string zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:added "0.4.0"} root-string
  "Return string representing the zipped-up `zloc` zipper."
  [zloc] (rewrite-clj.zip.base/root-string zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Renamed to [[root-string]]."
  [zloc] (rewrite-clj.zip.base/->root-string zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn print
  "Print current node in `zloc`.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  ([zloc writer] (rewrite-clj.zip.base/print zloc writer))
  ([zloc] (rewrite-clj.zip.base/print zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn print-root
  "Zip up and print `zloc` from root node.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  ([zloc writer] (rewrite-clj.zip.base/print-root zloc writer))
  ([zloc] (rewrite-clj.zip.base/print-root zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn replace
  "Return `zloc` with the current node replaced by `item`.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[replace*]] for non-coercing version of replace."
  [zloc item] (rewrite-clj.zip.editz/replace zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn edit
  "Return `zloc` with the current node replaced with the result of:

   `(apply f (s-expr current-node) args)`

  The result of `f`, if not already a node, will be coerced to a node if possible.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances).

  Use [[edit*]] for non-coercing version of edit."
  [zloc f & args] (apply rewrite-clj.zip.editz/edit zloc f args))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn splice
  "Return zipper with the children of the current node in `zloc` merged into itself.
   (akin to Clojure's `unquote-splicing` macro: `~@...`).
   - if the node is not one that can have children, no modification will
     be performed.
   - if the node has no or only whitespace children, it will be removed.
   - otherwise, splicing will be performed, moving the zipper to the first
     non-whitespace spliced child node.

  For example, given `[[1 2 3] 4 5 6]`, if zloc is located at vector `[1 2 3]`, a splice will result in raising the vector's children up `[1 2 3 4 5 6]` and locating the zipper at node `1`."
  [zloc] (rewrite-clj.zip.editz/splice zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn prefix
  "Return zipper with the current node in `zloc` prefixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, first line is prefixed."
  [zloc s] (rewrite-clj.zip.editz/prefix zloc s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.editz
(defn suffix
  "Return zipper with the current node in `zloc` suffixed with string `s`.
   Operates on token node or a multi-line node, else exception is thrown.
   When multi-line, last line is suffixed."
  [zloc s] (rewrite-clj.zip.editz/suffix zloc s))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.context
(defn reapply-context
  "Returns `zloc` with namespaced map sexpr context to all symbols and keywords reapplied from current location downward.

  Keywords and symbols:
  * that are keys in a namespaced map will have namespaced map context applied
  * otherwise will have any namespaced map context removed

  You should only need to use this function if:
  * you care about `sexpr` on keywords and symbols
  * and you are moving keywords and symbols from a namespaced map to some other location."
  [zloc] (rewrite-clj.zip.context/reapply-context zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find
  "Return `zloc` located to the first node satisfying predicate `p?` else nil.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next
  "Return `zloc` located to the next node satisfying predicate `p?` else `nil`.
   Search starts one movement `f` from the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-next zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-next zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-depth-first
  "Return `zloc` located to the first node satisfying predicate `p?` else `nil`.
   Search is depth-first from the current node."
  [zloc p?] (rewrite-clj.zip.findz/find-depth-first zloc p?))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-depth-first
  "Return `zloc` located to next node satisfying predicate `p?` else `nil`.
   Search starts depth-first after the current node."
  [zloc p?] (rewrite-clj.zip.findz/find-next-depth-first zloc p?))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-tag
  "Return `zloc` located to the first node with tag `t` else `nil`.
   Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc t] (rewrite-clj.zip.findz/find-tag zloc t))
  ([zloc f t] (rewrite-clj.zip.findz/find-tag zloc f t)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-tag
  "Return `zloc` located to the next node with tag `t` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc t] (rewrite-clj.zip.findz/find-next-tag zloc t))
  ([zloc f t] (rewrite-clj.zip.findz/find-next-tag zloc f t)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-value
  "Return `zloc` located to the first token node that `sexpr`esses to `v` else `nil`.
   Search starts from the current node and continues via movement function `f`.

   `v` can be a single value or a set. When `v` is a set, matches on any value in set.

   `f` defaults to [[right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v] (rewrite-clj.zip.findz/find-value zloc v))
  ([zloc f v] (rewrite-clj.zip.findz/find-value zloc f v)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-value
  "Return `zloc` located to the next token node that `sexpr`esses to `v` else `nil`.
   Search starts one movement `f` from the current location, and continues via `f`.

   `v` can be a single value or a set. When `v` is a set matches on any value in set.

   `f` defaults to [[right]] in short form call.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc v] (rewrite-clj.zip.findz/find-next-value zloc v))
  ([zloc f v] (rewrite-clj.zip.findz/find-next-value zloc f v)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-token
  "Return `zloc` located to the the first token node satisfying predicate `p?`.
  Search starts at the current node and continues via movement function `f`.

   `f` defaults to [[right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-token zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-token zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-next-token
  "Return `zloc` located to the next token node satisfying predicate `p?` else `nil`.
  Search starts one movement `f` after the current node and continues via `f`.

   `f` defaults to [[right]]"
  ([zloc p?] (rewrite-clj.zip.findz/find-next-token zloc p?))
  ([zloc f p?] (rewrite-clj.zip.findz/find-next-token zloc f p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-last-by-pos
  "Return `zloc` located to the last node spanning position `pos` that satisfies predicate `p?` else `nil`.
   Search is depth-first from the current node.

  NOTE: Does not ignore whitespace/comment nodes."
  ([zloc pos] (rewrite-clj.zip.findz/find-last-by-pos zloc pos))
  ([zloc pos p?] (rewrite-clj.zip.findz/find-last-by-pos zloc pos p?)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.findz
(defn find-tag-by-pos
  "Return `zloc` located to the last node spanning position `pos` with tag `t` else `nil`.
  Search is depth-first from the current node."
  [zloc pos t] (rewrite-clj.zip.findz/find-tag-by-pos zloc pos t))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.insert
(defn insert-right
  "Return zipper with `item` inserted to the right of the current node in `zloc`, without moving location.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Will insert a space if necessary.

  Use [[rewrite-clj.zip/insert-right*]] to insert without adding any whitespace."
  [zloc item] (rewrite-clj.zip.insert/insert-right zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.insert
(defn insert-left
  "Return zipper with `item` inserted to the left of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[insert-left*]] to insert without adding any whitespace."
  [zloc item] (rewrite-clj.zip.insert/insert-left zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.insert
(defn insert-child
  "Return zipper with `item` inserted as the first child of the current node in `zloc`, without moving location.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[insert-child*]] to insert without adding any whitespace."
  [zloc item] (rewrite-clj.zip.insert/insert-child zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.insert
(defn append-child
  "Return zipper with `item` inserted as the last child of the current node in `zloc`, without moving.
  Will insert a space if necessary.
  If `item` is not already a node, an attempt will be made to coerce it to one.

  Use [[append-child*]] to append without adding any whitespace."
  [zloc item] (rewrite-clj.zip.insert/append-child zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn left
  "Return zipper with location moved left to next non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc] (rewrite-clj.zip.move/left zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn right
  "Return zipper with location moved right to next non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc] (rewrite-clj.zip.move/right zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn up
  "Return zipper with location moved up to next non-whitespace/non-comment parent of current node in `zloc`, or `nil` if at the top."
  [zloc] (rewrite-clj.zip.move/up zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn down
  "Return zipper with location moved down to the first non-whitespace/non-comment child node of the current node in `zloc`, or nil if no applicable children."
  [zloc] (rewrite-clj.zip.move/down zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn prev
  "Return zipper with location moved to the previous depth-first non-whitespace/non-comment node in `zloc`. If already at root, returns nil."
  [zloc] (rewrite-clj.zip.move/prev zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn next
  "Return zipper with location moved to the next depth-first non-whitespace/non-comment node in `zloc`.
   End can be detected with [[end?]], if already at end, stays there."
  [zloc] (rewrite-clj.zip.move/next zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn leftmost
  "Return zipper with location moved to the leftmost non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc] (rewrite-clj.zip.move/leftmost zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn rightmost
  "Return zipper with location moved to the rightmost non-whitespace/non-comment sibling of current node in `zloc`."
  [zloc] (rewrite-clj.zip.move/rightmost zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn leftmost?
  "Return true if at leftmost non-whitespace/non-comment sibling node in `zloc`."
  [zloc] (rewrite-clj.zip.move/leftmost? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn rightmost?
  "Return true if at rightmost non-whitespace/non-comment sibling node in `zloc`."
  [zloc] (rewrite-clj.zip.move/rightmost? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.move
(defn end?
  "Return true if `zloc` is at end of depth-first traversal."
  [zloc] (rewrite-clj.zip.move/end? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.removez
(defn remove
  "Return `zloc` with current node removed. Returned zipper location
   is moved to the first non-whitespace node preceding removed node in a depth-first walk.
   Removes whitespace appropriately.

  - `[1 |2  3]    => [|1 3]`
  - `[1 |2]       => [|1]`
  - `[|1 2]       => |[2]`
  - `[|1]         => |[]`
  - `[  |1  ]     => |[]`
  - `[1 [2 3] |4] => [1 [2 |3]]`
  - `[|1 [2 3] 4] => |[[2 3] 4]`

   If the removed node is a rightmost sibling, both leading and trailing whitespace
   is removed, otherwise only trailing whitespace is removed.

   The result is that a following element (no matter whether it is on the same line
   or not) will end up at same positon (line/column) as the removed one.
   If a comment lies betwen the original node and the neighbour this will not hold true.

   If the removed node is at end of input and is trailed by 1 or more newlines,
   a single trailing newline will be preserved.

   Use [[remove*]] to remove node without removing any surrounding whitespace."
  [zloc] (rewrite-clj.zip.removez/remove zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.removez
(defn remove-preserve-newline
  "Same as [[remove]] but preserves newlines.
   Specifically: will trim all whitespace - or whitespace up to first linebreak if present."
  [zloc] (rewrite-clj.zip.removez/remove-preserve-newline zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn seq?
  "Returns true if current node in `zloc` is a sequence."
  [zloc] (rewrite-clj.zip.seqz/seq? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn list?
  "Returns true if current node in `zloc` is a list."
  [zloc] (rewrite-clj.zip.seqz/list? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn vector?
  "Returns true if current node in `zloc` is a vector."
  [zloc] (rewrite-clj.zip.seqz/vector? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn set?
  "Returns true if current node in `zloc` is a set."
  [zloc] (rewrite-clj.zip.seqz/set? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map?
  "Returns true if current node in `zloc` is a map."
  [zloc] (rewrite-clj.zip.seqz/map? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn namespaced-map?
  "Returns true if the current node in `zloc` is a namespaced map."
  [zloc] (rewrite-clj.zip.seqz/namespaced-map? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map
  "Returns `zloc` with function `f` applied to all nodes of the current node.
  Current node must be a sequence node. Equivalent to [[rewrite-clj.zip/map-vals]] for maps.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at
  - value nodes for maps
  - each element of a seq
  and is should return:
  - an updated zloc with zloc positioned at edited node
  - a falsey value to leave value node unchanged

  Folks typically use [[edit]] for `f`."
  [f zloc] (rewrite-clj.zip.seqz/map f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map-keys
  "Returns `zloc` with function `f` applied to all key nodes of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at key node and should return:
  - an updated zloc with zloc positioned at key node
  - a falsey value to leave value node unchanged

  Folks typically use [[rewrite-clj.zip/edit]] for `f`."
  [f zloc] (rewrite-clj.zip.seqz/map-keys f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn map-vals
  "Returns `zloc` with function `f` applied to each value node of the current node.
   Current node must be map node.

  `zloc` location is unchanged.

  `f` arg is zloc positioned at value node and should return:
  - an updated zloc with zloc positioned at value node
  - a falsey value to leave value node unchanged

  Folks typically use [[edit]] for `f`."
  [f zloc] (rewrite-clj.zip.seqz/map-vals f zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn get
  "Returns `zloc` located to map key node's sexpr value matching `k` else `nil`.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k] (rewrite-clj.zip.seqz/get zloc k))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.seqz
(defn assoc
  "Returns `zloc` with current node's `k` set to value `v`.

  `zloc` location is unchanged.

  `k` should be:
  - a key for maps
  - a zero-based index for sequences, an exception is thrown if index is out of bounds

  NOTE: `k` will be compared against resolved keywords in maps.
  See docs for sexpr behavior on [namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)."
  [zloc k v] (rewrite-clj.zip.seqz/assoc zloc k v))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defn edit-node
  "Return zipper applying function `f` to `zloc`. The resulting
   zipper will be located at the same path (i.e. the same number of
   downwards and right movements from the root) incoming `zloc`.

   See also [[subedit-node]] for an isolated edit."
  [zloc f] (rewrite-clj.zip.subedit/edit-node zloc f))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defn subedit-node
  "Return zipper replacing current node in `zloc` with result of `f` applied to said node as an isolated sub-tree.
   The resulting zipper will be located on the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc f] (rewrite-clj.zip.subedit/subedit-node zloc f))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defn subzip
  "Create and return a zipper whose root is the current node in `zloc`.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc] (rewrite-clj.zip.subedit/subzip zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defmacro edit->
  "Like `->`, threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`.

   See also [[subedit->]] for an isolated edit."
  [zloc & body] `(rewrite-clj.zip.subedit/edit-> ~zloc ~@body))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defmacro edit->>
  "Like `->>`, threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`.

   See also [[subedit->>]] for an isolated edit."
  [zloc & body] `(rewrite-clj.zip.subedit/edit->> ~zloc ~@body))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defmacro subedit->
  "Like `->`, threads `zloc`, as an isolated sub-tree through forms, then zips
   up to, and locates at, the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc & body] `(rewrite-clj.zip.subedit/subedit-> ~zloc ~@body))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.subedit
(defmacro subedit->>
  "Like `->`. Threads `zloc`, as an isolated sub-tree through forms, then zips
      up to, and locates at, the root of the modified sub-tree.

   See [docs on sub editing](/doc/01-user-guide.adoc#sub-editing)."
  [zloc & body] `(rewrite-clj.zip.subedit/subedit->> ~zloc ~@body))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.walk
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
  ([zloc f] (rewrite-clj.zip.walk/prewalk zloc f))
  ([zloc p? f] (rewrite-clj.zip.walk/prewalk zloc p? f)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.walk
(defn ^{:added "0.4.9"} postwalk
  "Return zipper modified by an isolated depth-first post-order traversal.

   Pre-order traversal visits children before root.
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
  ([zloc f] (rewrite-clj.zip.walk/postwalk zloc f))
  ([zloc p? f] (rewrite-clj.zip.walk/postwalk zloc p? f)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn whitespace?
  "Returns true when the current the node in `zloc` is a Clojure whitespace (which includes the comma)."
  [zloc] (rewrite-clj.zip.whitespace/whitespace? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn linebreak?
  "Returns true when the current node in `zloc` is a linebreak."
  [zloc] (rewrite-clj.zip.whitespace/linebreak? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn whitespace-or-comment?
  "Returns true when current node in `zloc` is whitespace or a comment."
  [zloc] (rewrite-clj.zip.whitespace/whitespace-or-comment? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn skip
  "Return zipper with location moved to first location not satisfying predicate `p?` starting from the node in
   `zloc` and traversing by function `f`."
  [f p? zloc] (rewrite-clj.zip.whitespace/skip f p? zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn skip-whitespace
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc`
   and traversing by function `f`.

   `f` defaults to [[right]]"
  ([zloc] (rewrite-clj.zip.whitespace/skip-whitespace zloc))
  ([f zloc] (rewrite-clj.zip.whitespace/skip-whitespace f zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn skip-whitespace-left
  "Return zipper with location moved to first non-whitespace/non-comment starting from current node in `zloc` traversing left."
  [zloc] (rewrite-clj.zip.whitespace/skip-whitespace-left zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:added "0.5.0"} insert-space-left
  "Return zipper with `n` space whitespace node inserted to the left of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (rewrite-clj.zip.whitespace/insert-space-left zloc))
  ([zloc n] (rewrite-clj.zip.whitespace/insert-space-left zloc n)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:added "0.5.0"} insert-space-right
  "Return zipper with `n` space whitespace node inserted to the right of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (rewrite-clj.zip.whitespace/insert-space-right zloc))
  ([zloc n] (rewrite-clj.zip.whitespace/insert-space-right zloc n)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:added "0.5.0"} insert-newline-left
  "Return zipper with `n` newlines node inserted to the left of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (rewrite-clj.zip.whitespace/insert-newline-left zloc))
  ([zloc n] (rewrite-clj.zip.whitespace/insert-newline-left zloc n)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:added "0.5.0"} insert-newline-right
  "Return zipper with `n` newlines node inserted to the right of the current node in `zloc`, without moving location.
   `n` defaults to 1."
  ([zloc] (rewrite-clj.zip.whitespace/insert-newline-right zloc))
  ([zloc n] (rewrite-clj.zip.whitespace/insert-newline-right zloc n)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:deprecated "0.5.0"} prepend-space
  "DEPRECATED: renamed to [[insert-space-left]]."
  ([zloc n] (rewrite-clj.zip.whitespace/prepend-space zloc n))
  ([zloc] (rewrite-clj.zip.whitespace/prepend-space zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:deprecated "0.5.0"} append-space
  "DEPRECATED: renamed to [[insert-space-right]]."
  ([zloc n] (rewrite-clj.zip.whitespace/append-space zloc n))
  ([zloc] (rewrite-clj.zip.whitespace/append-space zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:deprecated "0.5.0"} prepend-newline
  "DEPRECATED: renamed to [[insert-newline-left]]."
  ([zloc n] (rewrite-clj.zip.whitespace/prepend-newline zloc n))
  ([zloc] (rewrite-clj.zip.whitespace/prepend-newline zloc)))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.whitespace
(defn ^{:deprecated "0.5.0"} append-newline
  "DEPRECATED: renamed to [[insert-newline-right]]."
  ([zloc n] (rewrite-clj.zip.whitespace/append-newline zloc n))
  ([zloc] (rewrite-clj.zip.whitespace/append-newline zloc)))
#?(:clj
   
;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.zip.base
(defn of-file
  "Create and return zipper from all forms in Clojure/ClojureScript/EDN File `f`.

     Optional `opts` can specify:
     - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
     - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([f] (rewrite-clj.zip.base/of-file f))
  ([f opts] (rewrite-clj.zip.base/of-file f opts))))


;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn right*
  "Raw version of [[right]].

Returns zipper with location at the right sibling of the current node in `zloc`, or nil.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/right zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn left*
  "Raw version of [[left]].

Returns zipper with location at the left sibling of the current node in `zloc`, or nil.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/left zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn up*
  "Raw version of [[up]].

Returns zipper with the location at the parent of current node in `zloc`, or nil if at
  the top.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/up zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn down*
  "Raw version of [[down]].

Returns zipper with the location at the leftmost child of current node in `zloc`, or
  nil if no children.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/down zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn next*
  "Raw version of [[next]].

Returns zipper with location at the next depth-first location in the hierarchy in `zloc`.
  When reaching the end, returns a distinguished zipper detectable via [[end?]]. If already
  at the end, stays there.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/next zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn prev*
  "Raw version of [[prev]].

Returns zipper with location at the previous depth-first location in the hierarchy in `zloc`.
  If already at the root, returns nil.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/prev zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn rightmost*
  "Raw version of [[rightmost]].

Returns zipper with location at the rightmost sibling of the current node in `zloc`, or self.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/rightmost zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn leftmost*
  "Raw version of [[leftmost]].

Returns zipper with location at the leftmost sibling of the current node in `zloc`, or self.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/leftmost zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn remove*
  "Raw version of [[remove]].

Returns zipper with current node in `zloc` removed, with location at node that would have preceded
  it in a depth-first walk.

NOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc] (rewrite-clj.custom-zipper.core/remove zloc))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn replace*
  "Raw version of [[replace]].

Returns zipper with node `item` replacing current node in `zloc`, without moving location.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc item] (rewrite-clj.custom-zipper.core/replace zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn edit*
  "Raw version of [[edit]].

Returns zipper with value of `(apply f current-node args)` replacing current node in `zloc`.

   The result of `f` should be a rewrite-clj node.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc f & args] (apply rewrite-clj.custom-zipper.core/edit zloc f args))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn insert-left*
  "Raw version of [[insert-left]].

Returns zipper with node `item` inserted as the left sibling of current node in `zloc`,
 without moving location.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc item] (rewrite-clj.custom-zipper.core/insert-left zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn insert-right*
  "Raw version of [[insert-right]].

Returns zipper with node `item` inserted as the right sibling of the current node in `zloc`,
  without moving location.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc item] (rewrite-clj.custom-zipper.core/insert-right zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn insert-child*
  "Raw version of [[insert-child]].

Returns zipper with node `item` inserted as the leftmost child of the current node in `zloc`,
  without moving location.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc item] (rewrite-clj.custom-zipper.core/insert-child zloc item))

;; DO NOT EDIT FILE, automatically imported from: rewrite-clj.custom-zipper.core
(defn append-child*
  "Raw version of [[append-child]].

Returns zipper with node `item` inserted as the rightmost child of the current node in `zloc`,
  without moving.

NOTE: This function does no coercion, does not skip, nor provide any special handling for whitespace/comment nodes."
  [zloc item] (rewrite-clj.custom-zipper.core/append-child zloc item))
