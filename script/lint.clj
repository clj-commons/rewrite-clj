(ns lint
  (:require [lint-eastwood :as eastwood]
            [lint-kondo :as kondo]))

(defn task
  ;; TODO: repeated in lint-kondo, can I DRY?
  {:org.babashka/cli
   {:restrict true :restrict-args true
    :spec {:rebuild {:coerce :boolean
                     :desc "Force rebuild of clj-kondo lint cache"}}}}
  [opts]
  (kondo/lint opts)
  (eastwood/lint))
