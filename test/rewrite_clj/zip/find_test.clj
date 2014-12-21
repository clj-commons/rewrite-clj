(ns rewrite-clj.zip.find-test
  (:require [midje.sweet :refer :all]
            [fast-zip.core :as z]
            [rewrite-clj.zip
             [base :as base]
             [find :as f]]))

;; ## Fixture

(def root
  (base/of-string
    "(defn f\n  [x]\n  [(+ x 1)])"))

;; ## Tests

(let [is? (fn [sexpr]
            #(and (= (base/tag %) :token)
                  (= (base/sexpr %) sexpr)))]
  (fact "about 'find'."
        (let [loc (-> root z/down (f/find (is? 'y)))]
          loc => nil?)
        (let [loc (-> root z/down (f/find (is? 'defn)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'defn)
        (let [loc (-> root z/down (f/find (is? 'f)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'f)
        (let [loc (-> root z/down z/rightmost
                      (f/find z/left (is? 'f)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'f))
  (fact "about 'find-next'."
        (let [loc (-> root z/down (f/find-next (is? 'f)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'f)
        (let [loc (-> root z/down z/right z/right
                      (f/find-next (is? 'f)))]
          loc => nil?)
        (let [tks (->> (iterate
                         (fn [node]
                           (f/find-next
                             node
                             z/next
                             #(= (base/tag %) :token)))
                         root)
                       (take-while identity)
                       (rest))]
          (count tks) => 6
          (map base/sexpr tks) => '[defn f x + x 1]))
  (fact "about 'find-depth-first'."
        (let [loc (-> root (f/find-depth-first (is? 'f)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'f)
        (let [loc (-> root (f/find-depth-first (is? 'x)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'x))
  (fact "about 'find-next-depth-first'."
        (let [loc (-> root (f/find-next-depth-first (is? 'f)))]
          (base/tag loc) => :token
          (base/sexpr loc) => 'f)
        (let [tks (->> (iterate
                         (fn [node]
                           (f/find-next-depth-first
                             node
                             #(= (base/tag %) :token)))
                         root)
                       (take-while identity)
                       (rest))]
          (count tks) => 6
          (map base/sexpr tks) => '[defn f x + x 1])))

(fact "about 'find-tag'."
      (let [loc (-> root z/down (f/find-tag :vector))]
        (base/tag loc) => :vector
        (base/sexpr loc) => '[x])
      (let [loc (-> root z/down (f/find-tag :set))]
        loc => nil?))

(fact "about 'find-next-tag'."
      (let [loc (-> root z/down (f/find-next-tag :vector))]
        (base/tag loc) => :vector
        (base/sexpr loc) => '[x]
        (-> loc
            (f/find-next-tag :vector)
            base/sexpr) => '[(+ x 1)])
      (let [loc (-> root z/down (f/find-next-tag :set))]
        loc => nil?))

(fact "about 'find-token'."
      (let [loc (-> root z/down
                    (f/find-token
                      (comp #{'f 'defn} base/sexpr)))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'defn))

(fact "about 'find-next-token'."
      (let [loc (-> root z/down
                    (f/find-next-token
                      (comp #{'f 'defn} base/sexpr)))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'f)
      (let [locs (->> (iterate
                        (fn [node]
                          (f/find-next-token
                            node
                            z/next
                            (comp #{'x 'defn} base/sexpr)))
                        root)
                      (take-while identity)
                      (rest))]
        (map base/sexpr locs) => '[defn x x]))

(fact "about 'find-value'."
      (let [loc (-> root z/down (f/find-value 'f))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'f)
      (let [loc (-> root z/down (f/find-value 'y))]
        loc => nil?)
      (let [loc (-> root z/down (f/find-value #{'f 'defn}))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'defn))

(fact "about 'find-next-value'."
      (let [loc (-> root z/down (f/find-next-value 'f))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'f)
      (let [loc (-> root z/down (f/find-next-value 'y))]
        loc => nil?)
      (let [loc (-> root z/down
                    (f/find-next-value #{'f 'defn}))]
        (base/tag loc) => :token
        (base/sexpr loc) => 'f)
      (let [locs (->> (iterate
                        #(f/find-next-value
                           % z/next #{'x 'defn})
                        root)
                      (take-while identity)
                      (rest))]
        (map base/sexpr locs) => '[defn x x]))
