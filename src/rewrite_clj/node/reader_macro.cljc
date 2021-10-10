(ns ^:no-doc rewrite-clj.node.reader-macro
  (:require [rewrite-clj.node.protocols :as node]
            [rewrite-clj.node.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord ReaderNode [tag prefix suffix
                       sexpr-fn sexpr-count
                       children]
  node/Node
  (tag [_node] tag)
  (node-type [_node] :reader)
  (printable-only? [_node]
    (not sexpr-fn))
  (sexpr* [_node opts]
    (if sexpr-fn
      (sexpr-fn (node/sexprs children opts))
      (throw (ex-info "unsupported operation" {}))))
  (length [_node]
    (-> (node/sum-lengths children)
        (+ 1 (count prefix) (count suffix))))
  (string [_node]
    (str "#" prefix (node/concat-strings children) suffix))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [this children']
    (when sexpr-count
      (node/assert-sexpr-count children' sexpr-count))
    (assoc this :children children'))
  (leader-length [_node]
    (inc (count prefix)))

  Object
  (toString [node]
    (node/string node)))

(defrecord ReaderMacroNode [children]
  node/Node
  (tag [_node] :reader-macro)
  (node-type [_node] :reader-macro)
  (printable-only?[_node] false)
  (sexpr* [node _opts]
    (list 'read-string (node/string node)))
  (length [_node]
    (inc (node/sum-lengths children)))
  (string [_node]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))
  (leader-length [_node] 1)

  Object
  (toString [node]
    (node/string node)))

(defrecord DerefNode [children]
  node/Node
  (tag [_node] :deref)
  (node-type [_node] :deref)
  (printable-only?[_node] false)
  (sexpr* [_node opts]
    (list* 'deref (node/sexprs children opts)))
  (length [_node]
    (inc (node/sum-lengths children)))
  (string [_node]
    (str "@" (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 1)
    (assoc this :children children'))
  (leader-length [_node] 1)

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! ReaderNode)
(node/make-printable! ReaderMacroNode)
(node/make-printable! DerefNode)

;; ## Constructors

(defn- ->node
  [tag prefix suffix sexpr-fn sexpr-count children]
  (when sexpr-count
    (node/assert-sexpr-count children sexpr-count))
  (->ReaderNode
    tag prefix suffix
    sexpr-fn sexpr-count
    children))

(defn var-node
  "Create node representing a var where `children` is either a
   sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/var-node (n/token-node 'my-var))
       n/string)
   ;; => \"#'my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; prefix and the var
   (-> (n/var-node [(n/spaces 2)
                    (n/token-node 'my-var)])
       n/string)
   ;; => \"#'  my-var\"
   ```"
  [children]
  (if (sequential? children)
    (->node :var "'" "" #(list* 'var %) 1 children)
    (recur [children])))

(defn eval-node
  "Create node representing an inline evaluation
   where `children` is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/eval-node (n/list-node [(n/token-node 'inc)
                                  (n/spaces 1)
                                  (n/token-node 1)]))
       n/string)
   ;; => \"#=(inc 1)\"

   ;; specifying a sequence allows for whitespace between the
   ;; prefix and the form
   (-> (n/eval-node [(n/spaces 3)
                     (n/list-node [(n/token-node 'inc)
                                   (n/spaces 1)
                                   (n/token-node 1)])])
       n/string)
   ;; => \"#=   (inc 1)\"
   ```"
  [children]
  (if (sequential? children)
    (->node
      :eval "=" ""
      #(list 'eval (list* 'quote %))
      1 children)
    (recur [children])))

(defn reader-macro-node
  "Create node representing a reader macro with `macro-node` and `form-node` or `children`.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   ;; here we call with macro-node and form-node
   (-> (n/reader-macro-node (n/token-node 'my-macro)
                            (n/token-node 42))
       n/string)
   ;; => \"#my-macro 42\"

   ;; calling with a sequence of children gives us control over whitespace
   (-> (n/reader-macro-node [(n/token-node 'my-macro)
                             (n/spaces 4)
                             (n/token-node 42)])
       n/string)
   ;; => \"#my-macro    42\"
   ```"
  ([children]
   (->ReaderMacroNode children))
  ([macro-node form-node]
   (->ReaderMacroNode [macro-node (ws/spaces 1) form-node])))

(defn deref-node
  "Create node representing the dereferencing of a form
   where `children` is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/deref-node (n/token-node 'my-var))
       n/string)
   ;; => \"@my-var\"

   ;; specifying a sequence allows for whitespace between @ and form
   (-> (n/deref-node [(n/spaces 2)
                      (n/token-node 'my-var)])
       n/string)
   ;; => \"@  my-var\"
   ```"
  [children]
  (if (sequential? children)
    (->DerefNode children)
    (->DerefNode [children])))
