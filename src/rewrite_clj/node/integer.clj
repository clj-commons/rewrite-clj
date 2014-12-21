(ns rewrite-clj.node.integer
  (:require [rewrite-clj.node.protocols :as node]))

;; ## Node

(defrecord IntNode [value base]
  node/Node
  (tag [_] :token)
  (printable-only? [_] false)
  (sexpr [_] value)
  (length [this]
    (count (node/string this)))
  (string [_]
    (let [s (.toString (biginteger value) base)
          prefix (case base
                   8  "0"
                   10 ""
                   16 "0x"
                   (str base "r"))]
      (str prefix s)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! IntNode)

;; ## Constructor

(defn integer-node
  "Create node for an EDN integer with the given base."
  ([value]
   (integer-node value 10))
  ([value base]
   {:pre [(integer? value)
          (integer? base)
          (< 1 base 37)]}
   (->IntNode value base)))
