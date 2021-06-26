(ns lint
  (:require [helper.main :as main]
            [lint-eastwood :as eastwood]
            [lint-kondo :as kondo]))

(defn -main [& args]
  (when (main/doc-arg-opt kondo/args-usage args)
    (apply kondo/-main args)
    (eastwood/-main)))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
