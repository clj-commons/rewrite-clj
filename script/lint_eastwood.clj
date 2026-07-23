(ns lint-eastwood
  (:require [helper.cli :as cli]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn lint []
  (status/line :head "eastwood: linting")
  (shell/command "clojure -M:test-common:eastwood"))

(defn task
  {:org.babashka/cli cli/base-opts}
  []
  (lint))
