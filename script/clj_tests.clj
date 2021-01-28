#!/usr/bin/env bb

(ns clj_tests
  (:require [babashka.classpath :as cp]
            [clojure.string :as string]
            [clojure.tools.cli :as cli] ))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(def allowed-clojure-versions '("1.9" "1.10"))
(def default-clojure-version "1.10")

(def cli-options
  [["-v" "--clojure-version VERSION" (str "Clojure version" " [" (string/join ", " allowed-clojure-versions) "]")
    :default default-clojure-version
    :validate [#(some #{%} allowed-clojure-versions)
               (str "Must be one of: " (string/join ", " allowed-clojure-versions))]]
   ["-c" "--coverage" "Generate code coverage report"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Usage: cljs_test.clj <options>"
        options-summary]
       (string/join "\n")))

(defn error-msg [errors]
  (string/join "\n" errors))

(defn validate-args [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :exit-code 0}

      errors
      {:exit-message (error-msg errors) :exit-code 1}

      :else
      {:options options})))

(defn exit [code msg]
  (if (zero? code)
    (status/line :detail msg)
    (status/line :error msg))
  (System/exit code))

(defn run-tests[{:keys [:clojure-version :coverage]}]
  (let [cmd ["clojure"
             (str "-M:test-common:kaocha:" clojure-version)
             "--reporter" "documentation"]
        cmd (if coverage
              (concat cmd ["--plugin" "cloverage" "--codecov" "--cov-ns-exclude-regex" "rewrite-clj.potemkin.cljs"])
              cmd)]
    (if coverage
      (status/line :info (str "generating test coverage report against clojure v" clojure-version))
      (status/line :info (str "testing clojure source against clojure v" clojure-version)))
    (shell/command cmd)))

(defn main [args]
  (env/assert-min-versions)
  (let [{:keys [options exit-message exit-code]} (validate-args args)]
    (if exit-message
      (exit exit-code exit-message)
      (run-tests options)))
  nil)

(main *command-line-args*)
