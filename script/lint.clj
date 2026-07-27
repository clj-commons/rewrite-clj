(ns lint
  (:require [lint-eastwood :as eastwood]
            [lint-kondo :as kondo]))

(defn task
  {:org.babashka/cli kondo/cli-opts}
  [opts]
  (kondo/lint opts)
  (eastwood/lint))
