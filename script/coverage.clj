#!/usr/bin/env bb

(ns coverage
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn generate-doc-tests []
  (status/line :info "Generating tests for code blocks in documents")
  (shell/command ["clojure" "-X:test-doc-blocks" "gen-tests"]))

(defn run-clj-doc-tests []
  (status/line :info "Running unit and code block tests under Clojure for coverage report")
  (shell/command ["clojure" "-M:test-common:test-docs:kaocha"
                  "--plugin" "cloverage" "--codecov"
                  "--profile" "coverage"
                  "--no-randomize"
                  "--reporter" "documentation"]))

(defn main []
  (env/assert-min-versions)
  (generate-doc-tests)
  (run-clj-doc-tests)
  nil)

(main)
