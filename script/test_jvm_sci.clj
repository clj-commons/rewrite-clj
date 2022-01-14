#!/usr/bin/env bb

(ns test-jvm-sci
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def allowed-clojure-versions '("1.10" "1.11"))

(def args-usage "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [1.10, 1.11] [default: 1.10]
  --help                         Show this help")


(defn validate-opts [opts]
  (when (not (some #{(get opts "--clojure-version")} allowed-clojure-versions))
        (status/die 1 args-usage)))


(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (validate-opts opts)
    (let [clojure-version (get opts "--clojure-version")]

      (status/line :head "Exposing rewrite-clj API to sci")
      (shell/command "clojure -M:script -m sci-test-gen-publics")

      (status/line :head "Interpreting tests with sci from using JVM using Clojure %s" clojure-version)
      (shell/command (format "clojure -M:sci-test:%s -m sci-test.main --file script/sci_test_runner.clj --classpath test"
                             clojure-version))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
