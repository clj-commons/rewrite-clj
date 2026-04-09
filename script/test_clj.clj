#!/usr/bin/env bb

(ns test-clj
  (:require [clojure.string :as str]
            [helper.clojure-versions :as clojure-versions]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn run-unit-tests [{:keys [version alias] :as _clojure-version}]
  (status/line :head (str "testing clojure source against clojure v" version))
  (if (= "1.8" version)
    (shell/command "clojure"
                   (str "-M:test-common:clj-test-runner:" alias))
    (shell/command "clojure"
                   (str "-M:test-common:kaocha:" alias)
                   "--reporter" "documentation")))

(defn run-isolated-tests[{:keys [version alias] :as _clojure-version}]
  (status/line :head (str "running isolated tests against clojure v" version))
  (if (= "1.8" version)
    (shell/command "clojure" (str "-M:clj-test-runner:test-isolated:" alias)
                   "--dir" "test-isolated")
    (shell/command "clojure" (str "-M:kaocha:" alias)
                   "--profile" "test-isolated"
                   "--reporter" "documentation")))

(def cli-clojure-versions (conj (mapv :version (clojure-versions/all)) "all"))

(def args-usage (format "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [%s] [default: %s]
  --help                         Show this help"
                        (str/join ", " cli-clojure-versions)
                        (first cli-clojure-versions)))

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [clojure-version (get opts "--clojure-version")]
      (if (not (some #{clojure-version} cli-clojure-versions))
        (status/die 1 args-usage)
        (let [clojure-versions (if (= "all" clojure-version)
                                 (clojure-versions/all)
                                 [(clojure-versions/lookup clojure-version)])]
          (doseq [v clojure-versions]
            (run-unit-tests v)
            (run-isolated-tests v))))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
