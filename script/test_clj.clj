#!/usr/bin/env bb

(ns test-clj
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def allowed-clojure-versions '("1.8" "1.9" "1.10" "1.11"))

(defn run-unit-tests [clojure-version]
  (status/line :head (str "testing clojure source against clojure v" clojure-version))
  (if (= "1.8" clojure-version)
    (shell/command "clojure"
                   (str "-M:test-common:clj-test-runner:" clojure-version))
    (shell/command "clojure"
                   (str "-M:test-common:kaocha:" clojure-version)
                   "--reporter" "documentation")))

(defn run-isolated-tests[clojure-version]
  (status/line :head (str "running isolated tests against clojure v" clojure-version))
  (if (= "1.8" clojure-version)
    (shell/command "clojure" (str "-M:clj-test-runner:test-isolated:" clojure-version)
                   "--dir" "test-isolated")
    (shell/command "clojure" (str "-M:kaocha:" clojure-version)
                   "--profile" "test-isolated"
                   "--reporter" "documentation")))

(def args-usage "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [1.8, 1.9, 1.10, 1.11 all] [default: 1.8]
  --help                         Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [clojure-version (get opts "--clojure-version")]

      (if (not (some #{clojure-version} (conj allowed-clojure-versions "all")))
        (status/die 1 args-usage)
        (let [clojure-versions (if (= "all" clojure-version)
                                 allowed-clojure-versions
                                 [clojure-version])]
          (doseq [v clojure-versions]
            (run-unit-tests v)
            (run-isolated-tests v))))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
