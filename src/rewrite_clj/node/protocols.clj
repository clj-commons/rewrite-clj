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
    "How many characters appear before children?"))

(extend-protocol InnerNode
  Object
  (inner? [_] false)
  (children [_]
    (throw (UnsupportedOperationException.)))
  (replace-children [_ _]
    (throw (UnsupportedOperationException.)))
  (leader-length [_]
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

;; ## Defrecord Inheritance

(def ^:private parse-impls
  @#'clojure.core/parse-impls)

(defn base-method-signature [[name args & body]]
  [name (mapv #(if (#{'&} %) % '_)
              args)])

(defn args-from-signature [[name args]]
  (mapv (fn [s i]
          (if (= s '&)
            '&
            (symbol (str "_" i))))
        args
        (rest (range))))

(defn fully-qualify [sym]
  (if-let [r (resolve sym)]
    (if (class? r)
      (-> r str
          (clojure.string/replace "class " "")
          symbol)
      (-> r meta :ns str
          (str "/" (.sym r))
          symbol))
    sym))

(defn parse-base-impls [impls]
  (-> impls
      (->> (map (fn [[class-or-proto-or-iface method-sources]]
                  [(fully-qualify class-or-proto-or-iface)
                   (->> method-sources
                        (map (juxt base-method-signature
                                   (fn [x] `(~x))))
                        (into {}))]))
           (into {}))))

(defn proxy-method [[name args & body :as code]]
  (let [sym (gensym (str name "-"))
        qual (symbol (str *ns* "/" sym))
        sign (base-method-signature code)
        synth-args (args-from-signature sign)
        a (remove #{'&} synth-args)
        appl (if (= a synth-args)
               `(~qual ~@a)
               `(apply ~qual ~@a))]
    {:definition         `(defn ~sym ~args
                            ~@body)
     :proxied            `(~name ~synth-args
                            ~appl)}))

(defmacro defbase
  "A base is a way to represent and share implementations between records.
   specs is whatever could be written in a defrecord statement body.
   It will also define a tag protocol named (str name \"P\").
   See base-record"
  [name & specs]
  (let [proto-name (-> (str name "P") symbol fully-qualify)
        impls (parse-impls specs)
        proxies (map (fn [[class-or-proto-or-iface methods]]
                       [class-or-proto-or-iface
                        (map proxy-method methods)])
                     impls)
        proxy-defs (mapcat (fn [[_ proxs]]
                             (map :definition proxs))
                           proxies)
        proxied-impls (->> proxies
                           (map (fn [[cpi proxs]]
                                  [cpi (map :proxied proxs)]))
                           (into {}))
        base-def `(do (defprotocol ~proto-name) ;; tag protocol
                      ~@proxy-defs
                      (def ~name
                        (quote ~(-> proxied-impls
                                    parse-base-impls
                                    (assoc proto-name {})))))]
    base-def))

(defn defrecord-body-from-base [base]
  (->> base
       (mapcat (fn [[class-or-proto-or-iface m]]
                 (concat [class-or-proto-or-iface]
                         (apply concat (vals m)))))))

(defn merge-bases [a b]
  (->> [a b]
       (map (comp set keys))
       (apply clojure.set/union)
       (map (fn [k]
              [k (merge (get b k {})
                        (get a k {}))]))
       (into {})))

(defmacro defrecord-from-base
  "Expands to a defrecord statement by merging the base represetended by
  specs with abase."
  [aname abase fields & specs]
  (let [b @(resolve abase)
        z (-> specs parse-impls parse-base-impls)
        merged (merge-bases z @(resolve abase))
        code `(defrecord ~aname ~fields
                ~@(defrecord-body-from-base merged))]
    code))

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
                     count
                     inc))]
        [rows cols]))))

(defn ^:no-doc +extent
  [[row col] [row-extent col-extent]]
  [(+ row row-extent)
   (cond-> col-extent (zero? row-extent) (+ col))])

