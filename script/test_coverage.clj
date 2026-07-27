(ns test-coverage
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/command "clojure -X:test-doc-blocks gen-tests"))

(defn run-clj-doc-tests []
  (status/line :head "Running unit and code block tests under Clojure for coverage report")
  (shell/command "clojure" "-M:test-common:test-docs:kaocha"
                 "--plugin" "cloverage" "--codecov"
                 "--profile" "coverage"
                 "--no-randomize"
                 "--reporter" "documentation"))

(defn task
  [_opts]
  (generate-doc-tests)
  (run-clj-doc-tests))
