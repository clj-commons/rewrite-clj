#!/usr/bin/env bb

(ns test-doc
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/command "clojure -X:test-doc-blocks gen-tests"))

(defn run-clj-doc-tests []
  (status/line :head "Running code block tests under Clojure")
  (shell/command "clojure" "-M:test-common:test-docs:kaocha"
                 "--profile" "test-docs"
                 "--no-randomize"
                 "--reporter" "documentation"))

(defn run-cljs-doc-tests []
  (status/line :head "Running code block tests under ClojureScript")
  (let [compile-opts {:warnings {:fn-deprecated false :single-segment-namespace false}}
        opts-fname "target/cljsbuild/test/doc-tests-opts.edn"]
    (spit opts-fname compile-opts)
    (shell/command "clojure" "-M:test-common:test-docs:cljs:cljs-test"
                   "--compile-opts" opts-fname
                   "--dir" "target/test-doc-blocks/test"
                   "--out" "target/cljsbuild/doc-tests")))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (generate-doc-tests)
    (run-clj-doc-tests)
    (run-cljs-doc-tests))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
