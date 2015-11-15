(ns
  ^{:added "0.4.0"}
  rewrite-clj.node.protocols
  (:require [rewrite-clj.potemkin :refer [defprotocol+]]
            [clojure.string :as string]))

;; ## Node

(defprotocol+ Node
  "Protocol for EDN/Clojure nodes."
  (tag [_]
    "Keyword representing the type of the node.")
  (printable-only? [_]
    "Return true if the node cannot be converted to an s-expression
     element.")
  (sexpr [_]
    "Convert node to s-expression.")
  (length [_]
    "Get number of characters for the string version of this node.")
  (string [_]
    "Convert node to printable string."))

(extend-protocol Node
  Object
  (tag [_] :unknown)
  (printable-only? [_] false)
  (sexpr [this] this)
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexprs
  "Given a seq of nodes, convert those that represent s-expressions
   to the respective forms."
  [nodes]
  (->> nodes
       (remove printable-only?)
       (map sexpr)))

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
    "How many characters appear before children?")
  (trailer-length [_]
    "How many characters appear after children?"))

(extend-protocol InnerNode
  Object
  (inner? [_] false)
  (children [_]
    (throw (UnsupportedOperationException.)))
  (replace-children [_ _]
    (throw (UnsupportedOperationException.)))
  (leader-length [_]
    (throw (UnsupportedOperationException.)))
  (trailer-length [_]
    (throw (UnsupportedOperationException.))))

(defn child-sexprs
  "Get all child s-expressions for the given node."
  [node]
  (if (inner? node)
    (sexprs (children node))))

;; ## Coerceable

(defprotocol+ NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [_]))

;; ## Print Helper

(defn- ^:no-doc node->string
  ^String
  [node]
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (format "%n  %s%n"))
             (str " " n))]
    (format "<%s:%s>" (name (tag node)) n')))

(defn ^:no-doc write-node
  [^java.io.Writer writer node]
  (.write writer (node->string node)))

(defmacro ^:no-doc make-printable!
  [class]
  `(defmethod print-method ~class
     [node# w#]
     (write-node w# node#)))

;; ## Helpers

(defn ^:no-doc assert-sexpr-count
  [nodes c]
  (assert
    (= (count (remove printable-only? nodes)) c)
    (format "can only contain %d non-whitespace form%s."
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
                     count))]
        [rows cols]))))

(defn- adjust-child
  [[new-row new-col] node]
  (let [[rows cols] (extent node)
        next-row (+ new-row rows)
        next-col (if (zero? rows)
                   (+ new-col cols)
                   cols)]
    [[next-row next-col] (with-meta node {:row new-row
                                          :col new-col
                                          :next-row next-row
                                          :next-col next-col})]))

(defn ^:no-doc replace-children*
  [node children]
  (let [{:keys [row col] :or {row 1 col 1}} (meta node)
        [[next-row next-col] children'] (reduce
                                          (fn [[pos children] child]
                                            (let [[next-pos child'] (adjust-child pos child)]
                                              [next-pos (conj children child')]))
                                          [[row (+ col (leader-length node))] []]
                                          children)
        next-col (+ next-col (trailer-length node))]
   (-> node
     (assoc :children children')
     (with-meta {:row row
                 :col col
                 :next-row next-row
                 :next-col next-col}))))
