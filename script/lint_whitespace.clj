#!/usr/bin/env bb

(ns lint-whitespace
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- lint []
  (status/line :head "whitespace: linting")
  (shell/command "clojure -T:build lint-whitespace"))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (lint))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
