(ns rewrite-clj.node.node-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]))

(defspec t-nodes-with-children-report-accurate-leader-lengths
  (prop/for-all [node (g/node g/container-node-types)]
                (let [node-str (node/string node)
                      children-str (apply str (map node/string (node/children node)))
                      leader (node/leader-length node)]
                  (= (subs node-str leader (+ leader (count children-str)))
                     children-str))))
