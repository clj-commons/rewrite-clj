(ns rewrite-clj.node.generators
  (:require [clojure.set :as set]
            [clojure.test.check.generators :as gen]
            [rewrite-clj.interop :as interop]
            [rewrite-clj.node :as node]))

;; Leaf nodes

(def comment-node
  (gen/fmap
   (fn [[text eol]]
     (node/comment-node (str text eol)))
   (gen/tuple
    (gen/such-that
     #(re-matches #"[^\r\n]*" %)
     gen/string-ascii)
    (gen/elements ["" "\r" "\n"]))))

(def integer-node
  (gen/fmap
   (fn [[n base]]
     (node/integer-node n base))
   (gen/tuple
    (gen/choose (interop/min-int) (interop/max-int))
    (gen/choose 2 36))))

(def keyword-node
  (gen/fmap
   (fn [[kw auto-resolved?]]
     (node/keyword-node kw auto-resolved?))
   (gen/tuple
    gen/keyword
    gen/boolean)))

(def newline-node
  (gen/fmap
   (comp node/newline-node (partial apply str))
   (gen/vector (gen/elements [\newline \return]) 1 5)))

(def string-node
  (gen/fmap node/string-node gen/string-ascii))

(def token-node
  (gen/fmap node/token-node gen/symbol))

(def whitespace-node
  (gen/fmap
   (comp node/whitespace-node (partial apply str))
   (gen/vector (gen/elements [\space \tab]) 1 5)))

(def comma-node
  (gen/fmap
   (comp node/comma-node (partial apply str))
   (gen/vector (gen/return \,) 1 5)))

;; Container nodes

(def ^:private node-specs
  {;leaves           generator
   :comment          comment-node
   :integer          integer-node
   :keyword          keyword-node
   :newline          newline-node
   :string           string-node
   :token            token-node
   :whitespace       whitespace-node
   :comma            comma-node

   ;containers        < > ctor
   :deref            [1 1 node/deref-node]
   :eval             [1 1 node/eval-node]
   :fn               [1 5 node/fn-node]
   :forms            [0 5 node/forms-node]
   :list             [0 5 node/list-node]
   :map              [0 5 node/map-node]
   :meta             [2 2 node/meta-node]
   :quote            [1 1 node/quote-node]
   :raw-meta         [2 2 node/raw-meta-node]
   :reader-macro     [2 2 node/reader-macro-node]
   :set              [0 5 node/set-node]
   :syntax-quote     [1 1 node/syntax-quote-node]
   :uneval           [1 1 node/uneval-node]
   :unquote          [1 1 node/unquote-node]
   :unquote-splicing [1 1 node/unquote-splicing-node]
   :var              [1 1 node/var-node]
   :vector           [0 5 node/vector-node]})

(def all-node-types
  (into #{} (keys node-specs)))

(def leaf-node-types
  (->> node-specs
       (filter (comp gen/generator? second))
       (map first)
       (into #{})))

(def container-node-types
  (set/difference all-node-types leaf-node-types))

(def printable-only-types
  #{:comment
    :newline
    :whitespace
    :comma
    :uneval})

(def top-level-types
  #{:forms})

(defn- container*
  "Helper to generate a container type.  Generates from `min` to `max` usable
  nodes and from 0 to `(inc max)` printable-only nodes, then interleaves them
  randomly.  The containers constructor, `ctor`, is then applied to the
  resulting vector of children."
  [child-generator printable-only-generator [min max ctor]]
  (gen/fmap
   ctor
   (gen/fmap
    (fn [[children printable-only ordering]]
      (->> (concat children printable-only)
           (map vector ordering)
           (sort-by first)
           (map second)))
    (gen/tuple
     (gen/vector child-generator min max)
     (gen/vector printable-only-generator 0 (inc max))
     (gen/vector (gen/choose 0 (interop/max-int)) (+ max (inc max)))))))

(defn node
  "Generate parse nodes at random.

  `types` is a set of permissable top-level nodes.  If not specified, all
  known top-level nodes are allowed.  This does not affect non-top-level
  nodes.

  `depth` is the maximum depth of the tree.  It's possible the tree will be
  shallower if we pick a lot of leaf-type nodes, but this is a limit.  If
  not specified, the generator will pick depths from 1 through 5 at random.

  Current implementation notes:

   1. There's no serious attempt to nest things correctly.  For example, `#( ... )`
      forms may be nested, and unquote operators may appear outside
      syntax-quote forms. We do restrict forms nodes to be to level nodes only.

   2. Spaces and newlines are added at random, not in a smart way that
      allows the tree to be converted to a string and reparsed.  Symbols
      and constants will probably run together."
  ([]
   (node all-node-types))
  ([types]
   (gen/bind
    (gen/choose 1 5)
    (fn [depth]
      (node types depth))))
  ([types depth]
   (let [types (cond-> types
                 (zero? depth)
                 (set/intersection leaf-node-types))]
     (gen/bind
      (gen/elements types)
      (fn [type]
        (let [details (node-specs type)]
          (if (gen/generator? details)
            details
            (let [child-generator (node (set/difference all-node-types printable-only-types top-level-types) (dec depth))
                  printable-only-generator (node printable-only-types (dec depth))]
              (container* child-generator printable-only-generator details)))))))))

(comment
  ;; make sure we are generating :forms for test.check at only at top level (if at all)
  (let [c 1000000]
    (println "start")
    (->> (range 1 (inc c))
         (map
          (fn [n]
            (let [samp (first (gen/sample (node) 1))
                  nodes (tree-seq :children :children samp)
                  tags (into [] (map node/tag nodes))
                  ndx (.lastIndexOf tags :forms)]
              (when (= 0 (mod n 1000))
                (println n "of" c))
              ndx)))
         (frequencies)
         (println "\nmatch ndx frequencies (-1 and 0 are good)"))
    (println "done")))
