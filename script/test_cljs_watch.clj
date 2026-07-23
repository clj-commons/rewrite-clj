(ns test-cljs-watch
  (:require [helper.cli :as cli]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn task
  {:org.babashka/cli cli/base-opts}
  [_opts]
  (status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
  (shell/command "clojure -M:test-common:cljs:fig-test"))
