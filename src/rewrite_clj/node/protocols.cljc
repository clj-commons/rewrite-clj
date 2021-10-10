(ns ^:no-doc ^{:added "0.4.0"} rewrite-clj.node.protocols
  (:require [clojure.string :as string]
            [rewrite-clj.interop :as interop]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defprotocol Node
  "Protocol for EDN/Clojure/ClojureScript nodes."
  (tag [node]
    "Returns keyword representing type of `node`.")
  (node-type [node]
    "Returns keyword representing the node type for `node`.
     Currently internal and used to support testing.")
  (printable-only? [node]
    "Return true if `node` cannot be converted to an s-expression element.")
  (sexpr* [node opts]
    "Return `node` converted to form applying `opts`. Internal, use `sexpr` instead.")
  (length [node]
    "Return number of characters for the string version of `node`.")
  (string [node]
    "Return the string version of `node`."))

(extend-protocol Node
  #?(:clj Object :cljs default)
  (tag [_this] :unknown)
  (node-type [_this] :unknown)
  (printable-only? [_this] false)
  (sexpr* [this _opts] this)
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexpr-able?
  "Return true if [[sexpr]] is supported for `node`'s element type.

   See [related docs in user guide](/doc/01-user-guide.adoc#not-all-clojure-is-sexpr-able)"
  [node]
  (not (printable-only? node)))

(defn sexpr
  "Return `node` converted to form.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([node] (sexpr node {}))
  ([node opts] (sexpr* node opts)))

(defn sexprs
  "Return forms for `nodes`. Nodes that do not represent s-expression are skipped.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([nodes]
   (sexprs nodes {}))
  ([nodes opts]
   (->> nodes
        (remove printable-only?)
        (map #(sexpr % opts)))))

(defn sum-lengths
  "Return total string length for `nodes`."
  [nodes]
  (reduce + (map length nodes)))

(defn concat-strings
  "Return string version of `nodes`."
  [nodes]
  (reduce str (map string nodes)))

;; ## Inner Node

(defprotocol InnerNode
  "Protocol for non-leaf EDN/Clojure/ClojureScript nodes."
  (inner? [node]
    "Returns true if `node` can have children.")
  (children [node]
    "Returns child nodes for `node`.")
  (replace-children [node children]
    "Returns `node` replacing current children with `children`.")
  (leader-length [node]
    "Returns number of characters before children for `node`."))

(extend-protocol InnerNode
  #?(:clj Object :cljs default)
  (inner? [_this] false)
  (children [_this]
    (throw (ex-info "unsupported operation" {})))
  (replace-children [_this _children]
    (throw (ex-info "unsupported operation" {})))
  (leader-length [_this]
    (throw (ex-info "unsupported operation" {}))))

(defn child-sexprs
  "Returns children for `node` converted to Clojure forms.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node]
   (child-sexprs node {}))
  ([node opts]
   (when (inner? node)
     (sexprs (children node) opts))))

(defn node?
  "Returns true if `x` is a rewrite-clj created node."
  [x]
  (and (some? x) (not= :unknown (tag x))))

(defn default-auto-resolve [alias]
  (if (= :current alias)
    '?_current-ns_?
    (symbol (str "??_" alias "_??"))))

;; ## Coerceable

(defprotocol NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [form] "Coerce `form` to node."))

(defprotocol MapQualifiable
  "Protocol for nodes that can be namespaced map qualified"
  (map-context-apply [node map-qualifier]
    "Applies `map-qualifier` context to `node`")
  (map-context-clear [node]
    "Removes map-qualifier context for `node`"))

;; ## Print Helper

(defn- node->string
  #?(:clj ^String [node]
     :cljs ^string [node])
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (interop/simple-format "\n  %s\n"))
             (str " " n))]
    (interop/simple-format "<%s:%s>" (name (tag node)) n')))

#?(:clj
   (defn write-node
     [^java.io.Writer writer node]
     (.write writer (node->string node))))

#?(:clj
   (defmacro make-printable-clj!
     [class]
     `(defmethod print-method ~class
        [node# w#]
        (write-node w# node#)))
   :cljs
   (defn ^:no-doc make-printable-cljs!
     [obj]
     (extend-protocol IPrintWithWriter
       obj
       (-pr-writer [o writer _opts]
         (-write writer (node->string o))))))

(defn make-printable! [obj]
  #?(:clj (make-printable-clj! obj)
     :cljs (make-printable-cljs! obj)))

;; ## Helpers

(defn without-whitespace
  [nodes]
  (remove printable-only? nodes))

(defn assert-sexpr-count
  [nodes c]
  (assert
   (= (count (without-whitespace nodes)) c)
   (interop/simple-format "can only contain %d non-whitespace form%s."
                          c (if (= c 1) "" "s"))))

(defn assert-single-sexpr
  [nodes]
  (assert-sexpr-count nodes 1))

(defn extent
  "A node's extent is how far it moves the \"cursor\".

  Rows are simple - if we have x newlines in the string representation, we
  will always move the \"cursor\" x rows.

  Columns are strange.  If we have *any* newlines at all in the textual
  representation of a node, following nodes' column positions are not
  affected by our startting column position at all.  So the second number
  in the pair we return is interpreted as a relative column adjustment
  when the first number in the pair (rows) is zero, and as an absolute
  column position when rows is non-zero."
  [node]
  (let [{:keys [row col next-row next-col]} (meta node)]
    (if (and row col next-row next-col)
      [(- next-row row)
       (if (= row next-row row)
         (- next-col col)
         next-col)]
      (let [s (string node)
            rows (->> s (filter (partial = \newline)) count)
            cols (if (zero? rows)
                   (count s)
                   (->> s
                     reverse
                     (take-while (complement (partial = \newline)))
                     count
                     inc))]
        [rows cols]))))

(defn +extent
  [[row col] [row-extent col-extent]]
  [(+ row row-extent)
   (cond-> col-extent (zero? row-extent) (+ col))])

(defn meta-elided
  "Same as `clojure.core/meta` but with positional metadata removed.
  Use when you want to omit reader generated metadata on forms."
  [form]
  (apply dissoc (meta form) [:line :column :end-line :end-column]))

(defn value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
            (first)
            ((juxt tag sexpr)))
    (sexpr node)))
