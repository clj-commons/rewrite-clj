(ns
  ^{:added "0.4.0"}
  rewrite-clj.node.protocols
  (:require [clojure.string :as string]
            [rewrite-clj.interop :as interop]
            #?(:clj [rewrite-clj.potemkin.clojure :refer [defprotocol+]]))
  #?(:cljs (:require-macros [rewrite-clj.potemkin.cljs :refer [defprotocol+]])))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defprotocol+ Node
  "Protocol for EDN/Clojure nodes."
  (tag [_]
    "Keyword representing the type of the node.")
  (node-type [node]
    "Returns keyword representing the node type for `node`.
     Currently internal and used to support testing.")
  (printable-only? [_]
    "Return true if the node cannot be converted to an s-expression
     element.")
  (sexpr* [_node opts]
    "Convert node to s-expression.")
  (length [_]
    "Get number of characters for the string version of this node.")
  (string [_]
    "Convert node to printable string."))

(extend-protocol Node
  #?(:clj Object :cljs default)
  (tag [_] :unknown)
  (node-type [_this] :unknown)
  (printable-only? [_] false)
  (sexpr* [this _opts] this)
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexpr
  "Return `node` converted to form.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([node] (sexpr node {}))
  ([node opts] (sexpr* node opts)))

(defn sexprs
  "Return forms for `nodes`. Nodes that do not represent s-expression are skipped.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([nodes]
   (sexprs nodes {}))
  ([nodes opts]
   (->> nodes
        (remove printable-only?)
        (map #(sexpr % opts)))))

(defn ^:no-doc sum-lengths
  "Sum up lengths of the given nodes."
  [nodes]
  (reduce + (map length nodes)))

(defn ^:no-doc concat-strings
  "Convert nodes to strings and concatenate them."
  [nodes]
  (reduce str (map string nodes)))

;; ## Inner Node

(defprotocol+ InnerNode
  "Protocol for non-leaf EDN/Clojure nodes."
  (inner? [_]
    "Check whether the node can contain children.")
  (children [_]
    "Get child nodes.")
  (replace-children [_ children]
    "Replace the node's children.")
  (leader-length [_]
    "How many characters appear before children?"))

(extend-protocol InnerNode
  #?(:clj Object :cljs default)
  (inner? [_] false)
  (children [_]
    (throw (ex-info "unsupported operation" {})))
  (replace-children [_ _]
    (throw (ex-info "unsupported operation" {})))
  (leader-length [_]
    (throw (ex-info "unsupported operation" {}))))

(defn child-sexprs
  "Returns children for `node` converted to Clojure forms.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([node]
   (child-sexprs node {}))
  ([node opts]
   (when (inner? node)
     (sexprs (children node) opts))))

(defn node?
  ;; TODO: consider a marker interface instead?
  "Returns true if `x` is a rewrite-clj created node."
  [x]
  (not= :unknown (tag x)))

;; TODO: is this the right ns for this? Ok for now, it is internal
(defn default-auto-resolve [alias]
  (if (= :current alias)
    '?_current-ns_?
    (symbol (str "??_" alias "_??"))))

;; ## Coerceable

(defprotocol+ NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [_]))

(defprotocol+ MapQualifiable
  "Protocol for nodes that can be namespaced map qualified"
  (map-context-apply [node map-qualifier]
    "Applies `map-qualifier` context to `node`")
  (map-context-clear [node]
    "Removes map-qualifier context for `node`"))

;; ## Print Helper

(defn- ^:no-doc node->string
  ^String
  [node]
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (interop/simple-format "\n  %s\n"))
             (str " " n))]
    (interop/simple-format "<%s:%s>" (name (tag node)) n')))

#?(:clj
   (defn ^:no-doc write-node
     [^java.io.Writer writer node]
     (.write writer (node->string node))))

#?(:clj
   (defmacro ^:no-doc make-printable-clj!
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

(defn ^:no-doc without-whitespace
  [nodes]
  (remove printable-only? nodes))

(defn  ^:no-doc assert-sexpr-count
  [nodes c]
  (assert
   (= (count (without-whitespace nodes)) c)
   (interop/simple-format "can only contain %d non-whitespace form%s."
                          c (if (= c 1) "" "s"))))

(defn ^:no-doc assert-single-sexpr
  [nodes]
  (assert-sexpr-count nodes 1))

(defn ^:no-doc extent
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

(defn ^:no-doc +extent
  [[row col] [row-extent col-extent]]
  [(+ row row-extent)
   (cond-> col-extent (zero? row-extent) (+ col))])
