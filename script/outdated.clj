#!/usr/bin/env bb

(ns outdated
  (:require [antq.report :as antq-report]
            [antq.report.table]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def pin-clojure-version-pom "1.9.0")

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (if-let [outdated (->> (shell/command-no-exit ["clojure" "-M:outdated"] {:out :string})
                         :out
                         string/trim
                         edn/read-string
                         (remove #(and (= "pom.xml" (:file %))
                                       (= "org.clojure/clojure" (:name %))
                                       (= pin-clojure-version-pom (:version %))))
                         seq)]
    (antq-report/reporter outdated {:reporter "table"})
    (status/line :detail "All Clojure dependencies seem up to date.")))


(defn check-nodejs []
  (status/line :head "Checking Node.js deps")
  (when (not (.exists (io/file "node_modules")))
    (status/line :detail "node_modules, not found, installing.")
    (shell/command ["npm" "install"]))

  (let [{:keys [:exit]} (shell/command-no-exit ["npm" "outdated"])]
    (when (zero? exit)
      (status/line :detail "All Node.js dependencies seem up to date.")
      (status/line :detail "(warning: deps are only checked against installed ./node_modules)"))))

(defn -main[& args]
  (when (main/doc-arg-opt args)
    (check-clojure)
    (check-nodejs))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
