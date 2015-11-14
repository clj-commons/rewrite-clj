(ns rewrite-clj.node.generators
  (:require [clojure.test.check.generators :as gen]
            [rewrite-clj.node :as node]))

(def integer-node
  (gen/fmap
    (fn [[n base]]
      (node/integer-node n base))
    (gen/tuple
      (gen/choose Long/MIN_VALUE Long/MAX_VALUE)
      (gen/choose 2 36))))
