#!/usr/bin/env bb

(ns test-jvm-sci
  (:require [clojure.string :as str]
            [helper.clojure-versions :as clojure-versions]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def cli-clojure-versions (mapv :version (clojure-versions/for-native)))

(def args-usage (format "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [%s] [default: %s]
  --help                         Show this help"
                        (str/join ", " cli-clojure-versions)
                        (first cli-clojure-versions)))

(defn validate-opts [opts]
  (when (not (some #{(get opts "--clojure-version")} cli-clojure-versions))
        (status/die 1 args-usage)))

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (validate-opts opts)
    (let [clojure-version (clojure-versions/lookup (get opts "--clojure-version"))]
      (status/line :head "Exposing rewrite-clj API to sci")
      (shell/command "clojure -M:script -m sci-test-gen-publics")

      (status/line :head "Interpreting tests with sci from using JVM using Clojure %s" (:version clojure-version))
      (shell/command (format "clojure -M:sci-test:%s -m sci-test.main --file script/sci_test_runner.clj --classpath test"
                             (:alias clojure-version)))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
