#!/usr/bin/env bb

(ns lint-eastwood
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- lint []
  (status/line :head "eastwood: linting")
  (shell/command ["clojure" "-M:test-common:eastwood"]))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (lint))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
