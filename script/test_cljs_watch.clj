(ns test-cljs-watch
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn task
  [_opts]
  (status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
  (shell/command "clojure -M:test-common:cljs:fig-test"))
