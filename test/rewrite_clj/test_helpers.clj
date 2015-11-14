(ns rewrite-clj.test-helpers
  (:require [clojure.test.check :as tc]
            [midje.sweet :refer :all]))

(defn holds
  "A midje checker to check whether a property \"holds\".

  e.g. (fact (tc/quick-check ...) => holds)"
  [result]
  (= true (:result result)))

(defmacro property
  "Make a test.check property into a midje fact."
  ([descr prop]
   `(property ~descr 25 ~prop))
  ([descr trials prop]
  `(fact ~descr
     (tc/quick-check ~trials ~prop) => holds)))
