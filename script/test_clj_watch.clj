#!/usr/bin/env bb

(ns test-clj-watch
  (:require [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn -main [& args]
  (env/assert-min-versions)
  (status/line :head "launching kaocha watch on clojure sources")
  (shell/command (concat ["clojure" "-M:test-common:kaocha" "--watch"] args)))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
