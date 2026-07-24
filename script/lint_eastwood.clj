(ns lint-eastwood
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn lint []
  (status/line :head "eastwood: linting")
  (shell/command "clojure -M:test-common:eastwood"))

(defn task
  []
  (lint))
