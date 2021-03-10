(ns ^:no-doc rewrite-clj.node.extras
  (:require [rewrite-clj.node.comment :as ncomment]
            [rewrite-clj.node.whitespace :as nwhitespace]))

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (nwhitespace/whitespace? node)
      (ncomment/comment? node)))
