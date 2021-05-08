

#!/usr/bin/env bb

(ns test-clj
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def allowed-clojure-versions '("1.9" "1.10"))

(defn run-unit-tests [clojure-version]
  (status/line :head (str "testing clojure source against clojure v" clojure-version))
  (shell/command ["clojure"
                  (str "-M:test-common:kaocha:" clojure-version)
                  "--reporter" "documentation"]))

(defn run-isolated-tests[clojure-version]
  (status/line :head (str "running isolated tests against clojure v" clojure-version))
  (shell/command ["clojure" (str "-M:kaocha:" clojure-version)
                  "--profile" "test-isolated"
                  "--reporter" "documentation"]))

(def args-usage "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [1.9, 1.10] [default: 1.10]
  --help                         Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [clojure-version (get opts "--clojure-version")]
      (if (not (some #{clojure-version} allowed-clojure-versions))
        (status/die 1 args-usage)
        (do
          (run-unit-tests clojure-version)
          (run-isolated-tests clojure-version)))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
