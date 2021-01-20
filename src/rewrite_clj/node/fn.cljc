(ns ^:no-doc rewrite-clj.node.fn
  (:require [clojure.string :as string]
            [clojure.walk :as w]
            [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Conversion

(defn- construct-fn
  "Construct function form."
  [syms vararg body]
  (list
    'fn*
    (vec
      (concat
        syms
        (when vararg
          (list '& vararg))))
    body))

(defn- sym-index
  "Get index based on the substring following the parameter's `%`.
   Zero means vararg."
  [n]
  (cond (= n "&") 0
        (= n "") 1
        (re-matches #"\d+" n) (interop/str->int n)
        :else (throw (ex-info "arg literal must be %, %& or %integer." {}))))

(defn- symbol->gensym
  "If symbol starting with `%`, convert to respective gensym."
  [sym-seq vararg? max-n sym]
  (when (symbol? sym)
    (let [nm (name sym)]
      (when (string/starts-with? nm "%")
        (let [i (sym-index (subs nm 1))]
          (when (and (= i 0) (not @vararg?))
            (reset! vararg? true))
          (swap! max-n max i)
          (nth sym-seq i))))))

(defn- fn-walk
  "Walk the form and create an expand function form."
  [form]
  (let [syms (for [i (range)
                   :let [base (if (= i 0)
                                "rest__"
                                (str "p" i "__"))
                         s (name (gensym base))]]
               (symbol (str s "#")))
        vararg? (atom false)
        ;; TODO: an atom was the first interop solution I came up with when transcribing to cljs, review for something simpler?
        max-n (atom 0)
        body (w/prewalk
              #(or (symbol->gensym syms vararg? max-n %) %)
              form)]
    (construct-fn
     (take @max-n (rest syms))
     (when @vararg?
       (first syms))
     body)))

;; ## Node

(defrecord FnNode [children]
  node/Node
  (tag [_] :fn)
  (printable-only? [_]
    false)
  (sexpr [_]
    (fn-walk (node/sexprs children)))
  (length [_]
    (+ 3 (node/sum-lengths children)))
  (string [_]
    (str "#(" (node/concat-strings children) ")"))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    2)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! FnNode)

;; ## Constructor

(defn fn-node
  "Create node representing an anonymous function."
  [children]
  (->FnNode children))
