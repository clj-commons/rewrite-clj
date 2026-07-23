(ns test-jvm-sci
  (:require [clojure.string :as str]
            [helper.clojure-versions :as clojure-versions]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def cli-clojure-versions (mapv :version (clojure-versions/for-native)))

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
  (let [clojure-version (clojure-versions/lookup clojure-version)]
    (status/line :head "Exposing rewrite-clj API to sci")
    (shell/command "clojure -M:script -m sci-test-gen-publics")

    (status/line :head "Interpreting tests with sci from using JVM using Clojure %s" (:version clojure-version))
    (shell/command (format "clojure -M:sci-test:%s -m sci-test.main --file script/sci_test_runner.clj --classpath test"
                           (:alias clojure-version)))))
