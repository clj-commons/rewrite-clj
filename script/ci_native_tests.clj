(ns ci-native-tests
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [doric.core :as doric]
            [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]))

(def graal-versions ["25.3.4.1"])
(def oses ["ubuntu" "macos" "windows"])

(defn- ci-test-matrix []
  (for [os oses
        graal-version graal-versions
        test-task ["test-native" "test-native-sci"]
        clj-version (mapv :version (clojure-versions/for-native))]
    {:desc (str/join " " [test-task os (str "graal" graal-version) (str "clj" clj-version)])
     :cmd (str "bb " test-task " --clojure-version " clj-version)
     :os os
     :graal-version graal-version}))

(def valid-formats ["json" "table"])

(defn matrix-for-ci
  {:org.babashka/cli
   {:doc "Return a matrix for use within GitHub Actions workflow"
    :spec {:format {:coerce :string
                    :desc "Output format"
                    :enum valid-formats
                    :default (first valid-formats)}}}}
  [{:keys [format]}]
  (let [matrix (ci-test-matrix)]
    (if (= "json" format)
      (status/line :detail (json/generate-string matrix))
      (do
        (status/line :detail (doric/table [:os :graal-version :desc :cmd] matrix))
        (status/line :detail "Total jobs found: %d" (count matrix))))))
