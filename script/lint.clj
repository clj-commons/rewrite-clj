(ns lint
  (:require [helper.main :as main]
            [lint-eastwood :as eastwood]
            [lint-kondo :as kondo]
            [lint-whitespace :as whitespace]))

(defn -main [& args]
  (when (main/doc-arg-opt kondo/args-usage args)
    (whitespace/-main)
    (apply kondo/-main args)
    (eastwood/-main)))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
