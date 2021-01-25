#!/usr/bin/env bb

(ns sci_jvm_test
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(env/assert-min-versions)
(status/line :info "Exposing rewrite-clj API to sci")
(shell/command ["clojure" "-M:script" "-m" "sci-test-gen-publics"])

(status/line :info "Interpreting tests with sci from using JVM")
(shell/command ["clojure" "-M:sci-test" "-m" "sci-test.main" "--file" "script/sci_test_runner.clj" "--classpath" "test"])
nil
