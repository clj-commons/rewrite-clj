(ns rewrite-clj.node.replace-children-test
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [rewrite-clj.node.protocols :refer [extent]]
            [rewrite-clj.test-helpers :refer :all]))

(def node-and-replacement-children
  (gen/bind
    (gen/elements g/container-node-types)
    (fn [type]
      (gen/tuple
        (g/node #{type})
        (gen/fmap node/children (g/node #{type}))))))

(facts "about replacing children"

  (facts "replace-children preserves the meaning of the operation"
    (property "replace-children does not alter the number of children" 50
      (prop/for-all [[node children] node-and-replacement-children]
        (= (count children)
           (count (node/children (node/replace-children node children))))))
    (property "post-replace-children children are equivalent to the requested ones" 50
      (prop/for-all [[node children] node-and-replacement-children]
        (= (map node/string children)
           (map node/string (node/children (node/replace-children node children)))))))

  (property "replace-children does not affect the children's extents"
    (prop/for-all [[node children] node-and-replacement-children]
      (= (map extent children)
         (map extent (node/children (node/replace-children node children))))))

  (property "replace-children does not move the parent's starting position"
    (prop/for-all [[node children] node-and-replacement-children
                   row (gen/choose 1 25)
                   col (gen/choose 1 78)]
      (let [{after-row :row after-col :col} (-> node
                                              (with-meta {:row row :col col})
                                              (node/replace-children children)
                                              meta)]
        (= [row col] [after-row after-col]))))

  (property "first replaced child starts after leader"
    (prop/for-all [[node children] (gen/such-that
                                     (fn [[node children]]
                                       (not (zero? (count children))))
                                     node-and-replacement-children)]
      (let [updated-node (node/replace-children node children)
            {:keys [row col]} (meta updated-node)
            {child-row :row child-col :col} (meta (first (node/children updated-node)))]

        (= [row (+ col (node/leader-length updated-node))]
           [child-row child-col]))))

  (property "adjacency: all replaced children are adjacent"
    (prop/for-all [[node children] node-and-replacement-children]
      (->> (node/children (node/replace-children node children))
        (iterate rest)
        (take-while #(>= (count %) 2))
        (every?
          (fn [[a b]]
            (let [{:keys [next-row next-col]} (meta a)
                  {:keys [row col]} (meta b)]
              (= [next-row next-col] [row col]))))))))

(property "nodes with children report accurate leader lengths"
  (prop/for-all [node (g/node g/container-node-types)]
    (let [node-str (node/string node)
          children-str (apply str (map node/string (node/children node)))
          leader (node/leader-length node)]
      (= (subs node-str leader (+ leader (count children-str)))
         children-str))))

(property "nodes with children report accurate trailer lengths"
  (prop/for-all [node (g/node g/container-node-types)]
    (let [node-str (node/string node)
          children-str (apply str (map node/string (node/children node)))
          trailer (node/trailer-length node)]
      (= (subs node-str
               (- (count node-str) (count children-str) trailer)
               (- (count node-str) trailer))
         children-str))))
