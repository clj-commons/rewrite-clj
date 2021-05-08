#!/usr/bin/env bb

(ns outdated
  (:require [clojure.java.io :as io]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (shell/command-no-exit ["clojure" "-M:outdated"]))

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
