(ns rewrite-clj.zip.base
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [fast-zip.core :as z]))

;; ## Zipper

(defn edn*
  "Create zipper over the given Clojure/EDN node."
  [node]
  (z/zipper
    node/inner?
    node/children
    node/replace-children
    node))

(defn edn
  "Create zipper over the given Clojure/EDN node and move
   to the first non-whitespace/non-comment child."
  [node]
  (if (= (node/tag node) :forms)
    (-> (edn* node)
        (z/down)
        (ws/skip-whitespace))
    (recur (node/forms-node [node]))))

;; ## Inspection

(defn tag
  "Get tag of node at the current zipper location."
  [zloc]
  (some-> zloc z/node node/tag))

(defn sexpr
  "Get sexpr represented by the given node."
  [zloc]
  (some-> zloc z/node node/sexpr))
