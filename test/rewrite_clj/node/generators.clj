(ns rewrite-clj.node.generators
  (:require [clojure.set :as set]
            [clojure.test.check.generators :as gen]
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
      (gen/choose Long/MIN_VALUE Long/MAX_VALUE)
      (gen/choose 2 36))))

(def keyword-node
  (gen/fmap
    (fn [[kw namespaced?]]
      (node/keyword-node kw namespaced?))
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
    (gen/vector (gen/elements [\, \space \tab]) 1 5)))

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

   ;containers        < > ctor
   :deref            [1 1 node/deref-node            ]
   :eval             [1 1 node/eval-node             ]
   :fn               [1 5 node/fn-node               ]
   :forms            [0 5 node/forms-node            ]
   :list             [0 5 node/list-node             ]
   :map              [0 5 node/map-node              ]
   :meta             [2 2 node/meta-node             ]
   :quote            [1 1 node/quote-node            ]
   :raw-meta         [2 2 node/raw-meta-node         ]
   :reader-macro     [2 2 node/reader-macro-node     ]
   :set              [0 5 node/set-node              ]
   :syntax-quote     [1 1 node/syntax-quote-node     ]
   :uneval           [1 1 node/uneval-node           ]
   :unquote          [1 1 node/unquote-node          ]
   :unquote-splicing [1 1 node/unquote-splicing-node ]
   :var              [1 1 node/var-node              ]
   :vector           [0 5 node/vector-node           ]})


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
    :uneval})

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
        (gen/vector (gen/choose 0 Long/MAX_VALUE) (+ max (inc max)))))))

(defn node
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
             (let [child-generator (node (set/difference all-node-types printable-only-types) (dec depth))
                   printable-only-generator (node printable-only-types (dec depth))]
               (container* child-generator printable-only-generator details)))))))))
