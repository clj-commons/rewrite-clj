(ns ci-native-tests
  (:require [cheshire.core :as json]
            [doric.core :as doric]
            [helper.clojure-versions :as clojure-versions]
            [helper.main :as main]
            [lread.status-line :as status]))

(def java-versions ["25.0.2"])
(def oses ["ubuntu" "macos" "windows"])

(defn- ci-test-matrix []
  (for [os oses
        java-version java-versions
        test-task ["test-native" "test-native-sci"]
        clj-version (mapv :version (clojure-versions/for-native))]
    {:desc (str os ",jdk" java-version "," test-task ",clj" clj-version)
     :cmd (str "bb " test-task " --clojure-version " clj-version)
     :os os
     :java-version java-version}))

(def args-usage "Valid args:
  matrix-for-ci [--format=json]
  --help

Commands:
  matrix-for-ci Return a matrix for use within GitHub Actions workflow

Options:
  --help    Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [matrix (ci-test-matrix)]
      (if (= "json" (get opts "--format"))
        (status/line :detail (json/generate-string matrix))
        (do
          (status/line :detail (doric/table [:os :java-version :desc :cmd] matrix))
          (status/line :detail "Total jobs found: %d" (count matrix))))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
