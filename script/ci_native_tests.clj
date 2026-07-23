(ns ci-native-tests
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [doric.core :as doric]
            [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]))

(def java-versions ["25.1.3"])
(def oses ["ubuntu" "macos" "windows"])

(defn- ci-test-matrix []
  (for [os oses
        java-version java-versions
        test-task ["test-native" "test-native-sci"]
        clj-version (mapv :version (clojure-versions/for-native))]
    {:desc (str/join " " [test-task os (str "jdk" java-version) (str "clj" clj-version)])
     :cmd (str "bb " test-task " --clojure-version " clj-version)
     :os os
     :java-version java-version}))

(def valid-formats ["json" "table"])

(defn task
  {:org.babashka/cli
   {:restrict true :restrict-args true
    :spec {:format {:coerce :string
                    :desc (format "Output format [%s]" (str/join ", " valid-formats))
                    :default (first valid-formats)
                    :validate {:pred #(some #{%} valid-formats)
                               :ex-msg (fn [{:keys [value]}]
                                         (str "Invalid format: " value))}}}}}
  [{:keys [format]}]
  (let [matrix (ci-test-matrix)]
    (if (= "json" format)
      (status/line :detail (json/generate-string matrix))
      (do
        (status/line :detail (doric/table [:os :java-version :desc :cmd] matrix))
        (status/line :detail "Total jobs found: %d" (count matrix))))))
