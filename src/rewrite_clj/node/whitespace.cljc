(ns ^:no-doc rewrite-clj.node.whitespace
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.reader :as r])
  #?(:cljs (:require-macros rewrite-clj.node.whitespace)))

#?(:clj (set! *warn-on-reflection* true))

;; ## Newline Modifiers

(def ^:dynamic *newline-fn*
  "This function is applied to every newline string."
  identity)

(def ^:dynamic *count-fn*
  "This function is applied to every newline string and should produce
   the eventual character count."
  count)

#?(:clj
   (defmacro with-newline-fn
     [f & body]
     `(binding [*newline-fn* (comp *newline-fn* ~f)]
        ~@body)))

#?(:clj
   (defmacro with-count-fn
     [f & body]
     `(binding [*count-fn* (comp *count-fn* ~f)]
        ~@body)))

;; ## Nodes

(defn- sexpr-unsupported []
  (throw (ex-info "unsupported operation" {})) )

(defrecord WhitespaceNode [whitespace]
  node/Node
  (tag [_node] :whitespace)
  (node-type [_node] :whitespace)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (sexpr-unsupported))
  (length [_node]
    (count whitespace))
  (string [_node] whitespace)

  Object
  (toString [node]
    (node/string node)))

(defrecord CommaNode [commas]
  node/Node
  (tag [_node] :comma)
  (node-type [_node] :comma)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (sexpr-unsupported))
  (length [_node]
    (count commas))
  (string [_node] commas)

  Object
  (toString [node]
    (node/string node)))

(defrecord NewlineNode [newlines]
  node/Node
  (tag [_node] :newline)
  (node-type [_node] :newline)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (sexpr-unsupported))
  (length [_node]
    (*count-fn* newlines))
  (string [_node]
    (*newline-fn* newlines))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! WhitespaceNode)
(node/make-printable! CommaNode)
(node/make-printable! NewlineNode)

;; ## Constructors

(defn- string-of?
  [#?(:clj ^String s :default s) pred]
  (and s
       (string? s)
       (pos? (count s))
       (every? pred s)))

(defn whitespace-node
  "Create whitespace node of string `s`, where `s` is one or more space characters."
  [s]
  {:pre [(string-of? s r/space?)]}
  (->WhitespaceNode s))

(defn comma-node
  "Create comma node of string `s`, where `s` is one or more comma characters."
  [s]
  {:pre [(string-of? s r/comma?)]}
  (->CommaNode s))

(defn newline-node
  "Create newline node of string `s`, where `s` is one or more linebreak characters."
  [s]
  {:pre [(string-of? s r/linebreak?)]}
  (->NewlineNode s))

(defn- classify-whitespace
  [c]
  (cond (r/comma? c)     :comma
        (r/linebreak? c) :newline
        :else :whitespace))

(defn whitespace-nodes
  "Convert string `s` of whitespace to whitespace/newline nodes."
  [s]
  {:pre [(string-of? s r/whitespace?)]}
  (->> (partition-by classify-whitespace s)
       (map
         (fn [char-seq]
           (let [s (apply str char-seq)]
             (case (classify-whitespace (first char-seq))
               :comma   (comma-node s)
               :newline (newline-node s)
               (whitespace-node s)))))))

;; ## Utilities

(defn spaces
  "Create node representing `n` spaces."
  [n]
  (whitespace-node (apply str (repeat n \space))))

(defn newlines
  "Create node representing `n` newline characters."
  [n]
  (newline-node (apply str (repeat n \newline))))

(let [comma (whitespace-nodes ", ")]
  (defn comma-separated
    "Interleave `nodes` with `\", \"` nodes."
    [nodes]
    (->> nodes
         (mapcat #(cons % comma))
         (drop-last (count comma)))))

(let [nl (newline-node "\n")]
  (defn line-separated
    "Interleave `nodes` with newline nodes."
    [nodes]
    (butlast (interleave nodes (repeat nl)))))

(let [space (whitespace-node " ")]
  (defn space-separated
    "Interleave `nodes` with `\" \"` nodes."
    [nodes]
    (butlast (interleave nodes (repeat space)))))

;; ## Predicates

(defn whitespace?
  "Returns true if `node` represents Clojure whitespace."
  [node]
  (contains?
   #{:whitespace
     :newline
     :comma}
   (node/tag node)))

(defn linebreak?
  "Returns true if `node` represents one or more linebreaks."
  [node]
  (= (node/tag node) :newline))

(defn comma?
  "Returns true if `node` represents one or more commas."
  [node]
  (= (node/tag node) :comma))
