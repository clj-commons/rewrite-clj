(ns ^:no-doc rewrite-clj.node.comment
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord CommentNode [s]
  node/Node
  (tag [_node] :comment)
  (node-type [_node] :comment)
  (printable-only? [_node] true)
  (sexpr* [_node _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [_node]
    (+ 1 (count s)))
  (string [_node]
    (str ";" s))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! CommentNode)

;; ## Constructor

(defn comment-node
  "Create node representing a comment with text `s`.

   `s` should:
   - not specify the first leading semicolon
   - usually include the trailing newline character, otherwise subsequent nodes will be on the comment line

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/comment-node \"; my comment\\n\")
       n/string)
   ;; => \";; my comment\\n\"
   ```"
  [s]
  {:pre [(re-matches #"[^\r\n]*[\r\n]?" s)]}
  (->CommentNode s))

(defn comment?
  "Returns true if `node` is a comment."
  [node]
  (= (node/tag node) :comment))
