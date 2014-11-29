(ns rewrite-clj.zip.utils-test
  (:require [midje.sweet :refer :all]
            [fast-zip.core :as z]
            [rewrite-clj.zip.utils :refer :all]))

(let [loc (z/down (z/vector-zip '[a b c d]))]
  (fact "about 'remove-right'."
        (let [loc' (remove-right loc)]
          (z/node loc') => 'a
          (z/root loc') => '[a c d]))
  (fact "about 'remove-left'."
        (let [loc' (-> loc z/right z/right remove-left)]
          (z/node loc') => 'c
          (z/root loc') => '[a c d]))
  (fact "about 'remove-and-move-right'."
        (let [loc' (remove-and-move-right (z/right loc))]
          (z/node loc') => 'c
          (z/root loc') => '[a c d]))
  (fact "about 'remove-and-move-left'."
        (let [loc' (-> loc z/right remove-and-move-left)]
          (z/node loc') => 'a
          (z/root loc') => '[a c d])))
