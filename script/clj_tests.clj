#!/usr/bin/env bb

(ns clj-tests
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def allowed-clojure-versions '("1.9" "1.10"))
(def default-clojure-version "1.10")

(def cli-options
  [["-v" "--clojure-version VERSION" (str "Clojure version" " [" (string/join ", " allowed-clojure-versions) "]")
    :default default-clojure-version
    :validate [#(some #{%} allowed-clojure-versions)
               (str "Must be one of: " (string/join ", " allowed-clojure-versions))]]
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
    (status/die code msg)))

(defn run-unit-tests [{:keys [:clojure-version]}]
  (status/line :head (str "testing clojure source against clojure v" clojure-version))
  (shell/command ["clojure"
                  (str "-M:test-common:kaocha:" clojure-version)
                  "--reporter" "documentation"]))

(defn run-isolated-tests[{:keys [:clojure-version]}]
  (status/line :head (str "running isolated tests against clojure v" clojure-version))
  (shell/command ["clojure" (str "-M:kaocha:" clojure-version)
                  "--profile" "test-isolated"
                  "--reporter" "documentation"]))

(defn -main [& args]
  (env/assert-min-versions)
  (let [{:keys [options exit-message exit-code]} (validate-args args)]
    (if exit-message
      (exit exit-code exit-message)
      (do (run-unit-tests options)
          (run-isolated-tests options))))
  nil)

(env/when-invoked-as-script
 (apply -main *command-line-args*))
