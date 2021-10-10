(ns ^:no-doc rewrite-clj.node.integer
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord IntNode [value base]
  node/Node
  (tag [_node] :token)
  (node-type [_node] :int)
  (printable-only? [_node] false)
  (sexpr* [_node _opts] value)
  (length [node]
    (count (node/string node)))
  (string [_node]
    (let [sign (when (< value 0)
                 "-")
          abs-value (cond-> value (< value 0) -)
          s (interop/int->str abs-value base)
          prefix (case (long base)
                   8  "0"
                   10 ""
                   16 "0x"
                   (str base "r"))]
      (str sign prefix s)))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! IntNode)

;; ## Constructor

(defn integer-node
  "Create node representing an integer `value` in `base`.

  `base` defaults to 10.

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/integer-node 42)
       n/string)
   ;; => \"42\"

   (-> (n/integer-node 31 2)
       n/string)
   ;; => \"2r11111\"
   ```

   Note: the parser does not currently parse to integer-nodes, but they fully supported for output."
  ([value]
   (integer-node value 10))
  ([value base]
   {:pre [(integer? value)
          (integer? base)
          (< 1 base 37)]}
   (->IntNode value base)))
