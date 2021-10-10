(ns ^:no-doc rewrite-clj.node.quote
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord QuoteNode [tag prefix sym children]
  node/Node
  (tag [_node] tag)
  (node-type [_node] :quote)
  (printable-only? [_node] false)
  (sexpr* [_node opts]
    (list sym (first (node/sexprs children opts))))
  (length [_node]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_node]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (node/assert-single-sexpr children')
    (assoc node :children children'))
  (leader-length [_node]
    (count prefix))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! QuoteNode)

;; ## Constructors

(defn- ->node
  [t prefix sym children]
  (node/assert-single-sexpr children)
  (->QuoteNode t prefix sym children))

(defn quote-node
  "Create node representing a single quoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/quote-node (n/token-node 'sym))
       (n/string))
   ;; => \"'sym\"

   ;; specifying a sequence allows for whitespace between the
   ;; quote and the quoted
   (-> (n/quote-node [(n/spaces 10)
                      (n/token-node 'sym1) ])
       n/string)
   ;; => \"'          sym1\"
   ```"
  [children]
  (if (sequential? children)
    (->node
      :quote "'" 'quote
      children)
    (recur [children])))

(defn syntax-quote-node
  "Create node representing a single syntax-quoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/syntax-quote-node (n/token-node 'map))
       n/string)
   ;; => \"`map\"

   ;; specifying a sequence allows for whitespace between the
   ;; syntax quote and the syntax quoted
   (-> (n/syntax-quote-node [(n/spaces 3)
                             (n/token-node 'map)])
       n/string)
   ;; => \"`   map\"
   ```"
  [children]
  (if (sequential? children)
    (->node
      :syntax-quote "`" 'quote
      children)
    (recur [children])))

(defn unquote-node
  "Create node representing a single unquoted form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/unquote-node (n/token-node 'my-var))
       n/string)
   ;; => \"~my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; unquote and the uquoted
   (-> (n/unquote-node [(n/spaces 4)
                        (n/token-node 'my-var)])
       n/string)
   ;; => \"~    my-var\"
   ```"
  [children]
  (if (sequential? children)
    (->node
      :unquote "~" 'unquote
      children)
    (recur [children])))

(defn unquote-splicing-node
  "Create node representing a single unquote-spliced form where `children`
   is either a sequence of nodes or a single node.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/unquote-splicing-node (n/token-node 'my-var))
       n/string)
   ;; => \"~@my-var\"

   ;; specifying a sequence allows for whitespace between the
   ;; splicing unquote and the splicing unquoted
   (-> (n/unquote-splicing-node [(n/spaces 2)
                                 (n/token-node 'my-var)])
       n/string)
   ;; => \"~@  my-var\"
   ```"
  [children]
  (if (sequential? children)
    (->node
      :unquote-splicing "~@" 'unquote-splicing
      children)
    (recur [children])))
