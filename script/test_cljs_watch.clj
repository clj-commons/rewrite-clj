#!/usr/bin/env bb

(ns test-cljs-watch
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))


(defn -main [& args]
  (when (main/doc-arg-opt args)
    (status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
    (shell/command ["clojure" "-M:test-common:cljs:fig-test"])))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
