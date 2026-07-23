(ns test-clj
  (:require [clojure.string :as str]
            [helper.clojure-versions :as clojure-versions]
            [helper.jdk :as jdk]
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

(defn task
  {:org.babashka/cli
   {:restrict true :restrict-args true
    :spec {:clojure-version {:alias :v
                             :coerce :string
                             :desc (format "Test with Clojure [%s]" (str/join ", " cli-clojure-versions))
                             :default (first cli-clojure-versions)
                             :validate {:pred #(some #{%} cli-clojure-versions)
                                        :ex-msg (fn [{:keys [value]}]
                                                  (str "Invalid clojure version: " value))}}}}}
  [{:keys [clojure-version]}]
  (let [env-jdk-version (jdk/version)
        clojure-versions (if (= "all" clojure-version)
                             (clojure-versions/all)
                             [(clojure-versions/lookup clojure-version)])]
    (doseq [v clojure-versions]
      (if (and (= "all" clojure-version)
               (< (:major env-jdk-version) (:min-jdk-major v)))
        (status/line :warn "Skipping testing clojure version %s\nIt requires min JDK %s, found JDK %s"
                     (:mvn-version v) (:min-jdk-major v) (:version env-jdk-version))
        (do
          (run-unit-tests v)
          (run-isolated-tests v))))))
