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
    "Replace the node's children."))

(extend-protocol InnerNode
  Object
  (inner? [_] false)
  (children [_]
    (throw (UnsupportedOperationException.)))
  (replace-children [_ _]
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

(defn- adjust-child
  [[new-row new-col] node]
  ;; TODO: Fix the case where we don't have existing metadata information
  ;; for the node by computing the number of rows/cols.
  
  ;; TODO: Ensure new-row/new-col won't ever be nil and remove them from
  ;; the check.

  ;; TODO: Remove the `and` check entirely after handling all cases.

  ;; TODO: Column handling is dependent on row handling; e.g. if the node
  ;; spans more than one row, the column delta should not be applied.
  (let [{:keys [row col next-row next-col]} (meta node)]
    (if (and new-row new-col row col next-row next-col)
      (let [row-delta (- new-row row)
            col-delta (- new-col col)
            next-row (+ next-row row-delta)
            next-col (+ next-col col-delta)]
        [[next-row next-col] (with-meta node {:row new-row
                                              :col new-col
                                              :next-row next-row
                                              :next-col next-col})])
    [[new-row new-col] node])))

(defn ^:no-doc replace-children*
  [node children]
  (let [{:keys [row col]} (meta node)
        [[next-row next-col] children'] (reduce
                                          (fn [[pos children] child]
                                            (let [[next-pos child'] (adjust-child pos child)]
                                              [next-pos (conj children child)]))
                                          [[row col] []]
                                          children)]
   (-> node
     (assoc :children children')
     (with-meta {:row row
                 :col col
                 :next-row next-row
                 :next-col next-col}))))
