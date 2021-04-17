#!/usr/bin/env bb

(ns cljs-watch
  (:require [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn -main []
  (env/assert-min-versions)
  (status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
  (shell/command ["clojure" "-M:test-common:cljs:fig-test"]))

(env/when-invoked-as-script
 (-main))
