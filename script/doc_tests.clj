#!/usr/bin/env bb

(ns doc_tests
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn generate-doc-tests []
  (status/line :info "Generating tests for code blocks in documents")
  (shell/command ["clojure" "-X:test-doc-blocks" "gen-tests"]))

(defn run-clj-doc-tests []
  (status/line :info "Running code block tests under Clojure")
  (shell/command ["clojure" "-M:test-common:test-docs:kaocha"
                  "--profile" "test-docs"
                  "--no-randomize"
                  "--reporter" "documentation"]))

(defn run-cljs-doc-tests []
  (status/line :info "Running code block tests under ClojureScript")
  (shell/command ["clojure" "-M:test-common:test-docs:cljs-test"
                  "--compile-opts" "{:warnings {:fn-deprecated false :single-segment-namespace false}}"
                  "--dir" "target/test-doc-blocks/test"
                  "--out" "target/cljsbuild/doc-tests"]))

(defn main []
  (env/assert-min-versions)
  (generate-doc-tests)
  (run-clj-doc-tests)
  (run-cljs-doc-tests)
  nil)

(main)
