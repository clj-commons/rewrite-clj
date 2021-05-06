(ns helper.main
  (:require [helper.env :as env]
            [lread.status-line :as status]))

(defn run-argless-cmd
  "Some commands accept no args. This little helper is for them to keep things a bit more DRY."
  [args f]
  (env/assert-min-versions)
  (let [usage "This command accepts no arguments."]
    (cond
      (and (= 1 (count args)) (= "--help" (first args)))
      (status/line :detail usage)

      (> (count args) 0)
      (status/die 1 usage)

      :else
      (f))))
