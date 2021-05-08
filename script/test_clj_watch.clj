#!/usr/bin/env bb

(ns test-clj-watch
  (:require [helper.env :as env]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn -main [& args]
  ;; we simply pass along any args to kaocha, it will validate them
  (env/assert-min-versions)
  (status/line :head "launching kaocha watch on clojure sources")
  (shell/command (concat ["clojure" "-M:test-common:kaocha" "--watch"] args)))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
