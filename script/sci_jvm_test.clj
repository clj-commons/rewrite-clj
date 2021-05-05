#!/usr/bin/env bb

(ns sci-jvm-test
  (:require [helper.env :as env]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn -main [& args]
  (main/run-argless-cmd
   args
   (fn []
     (status/line :head "Exposing rewrite-clj API to sci")
     (shell/command ["clojure" "-M:script" "-m" "sci-test-gen-publics"])

     (status/line :head "Interpreting tests with sci from using JVM")
     (shell/command ["clojure" "-M:sci-test" "-m" "sci-test.main" "--file" "script/sci_test_runner.clj" "--classpath" "test"])))
  nil)

(env/when-invoked-as-script
 (apply -main *command-line-args*))
