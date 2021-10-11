#!/usr/bin/env bb

(ns lint-whitespace
  (:require [helper.jdk :as jdk]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- lint []
  (status/line :head "whitespace: linting")
  (let [jdk-version (jdk/version)]
    (if (< (:major jdk-version) 11)
      (status/line :warn "Skipping linting, min of JDK 11 required, detected JDK %s" (:version jdk-version))
      (shell/command "clojure -T:build lint-whitespace"))))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (lint))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
