(ns ^:no-doc rewrite-clj.node.fn
  (:require [clojure.string :as string]
            [clojure.walk :as w]
            [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Conversion

(defn- construct-fn
  "Construct function form."
  [fixed-arg-syms vararg-sym body]
  (list
    'fn*
    (vec
      (concat
        fixed-arg-syms
        (when vararg-sym
          (list '& vararg-sym))))
    body))

(defn- arg-index
  "Get index based on the substring following the arg's `%`.
   Zero means vararg."
  [n]
  (cond (= n "&") 0
        (= n "") 1
        (re-matches #"\d+" n) (interop/str->int n)
        :else (throw (ex-info "arg literal must be %, %& or %integer." {}))))

(defn- arg-symbol->gensym
  "If symbol starting with `%`, convert to respective gensym."
  [gensym-seq vararg? max-fixed-arg-ndx sym]
  (when (symbol? sym)
    (let [nm (name sym)]
      (when (string/starts-with? nm "%")
        (let [param-ndx (arg-index (subs nm 1))]
          (when (and (= param-ndx 0) (not @vararg?))
            (reset! vararg? true))
          (swap! max-fixed-arg-ndx max param-ndx)
          (nth gensym-seq param-ndx))))))

(defn- fn-walk
  "Walk the form and create an expand function form."
  [form]
  (let [sym-seq (for [i (range)
                      :let [base (if (= i 0)
                                   "rest__"
                                   (str "p" i "__"))
                            s (name (gensym base))]]
                  (symbol (str s "#")))
        max-fixed-arg-ndx (atom 0)
        vararg? (atom false)
        body (w/prewalk
              #(or (arg-symbol->gensym sym-seq vararg? max-fixed-arg-ndx %) %)
              form)]
    (construct-fn
     (take @max-fixed-arg-ndx (rest sym-seq))
     (when @vararg?
       (first sym-seq))
     body)))

;; ## Node

(defrecord FnNode [children]
  node/Node
  (tag [_node] :fn)
  (node-type [_node] :fn)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (fn-walk (node/sexprs children opts)))
  (length [_node]
    (+ 3 (node/sum-lengths children)))
  (string [_node]
    (str "#(" (node/concat-strings children) ")"))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (assoc node :children children'))
  (leader-length [_node] 2)

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! FnNode)

;; ## Constructor

(defn fn-node
  "Create node representing an anonymous function with `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/fn-node [(n/token-node '+)
                   (n/spaces 1)
                   (n/token-node 1)
                   (n/spaces 1)
                   (n/token-node '%1)])
       n/string)
   ;; => \"#(+ 1 %1)\"
   ```"
  [children]
  (->FnNode children))
