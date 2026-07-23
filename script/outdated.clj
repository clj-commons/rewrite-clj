(ns outdated
  (:require [clojure.java.io :as io]
            [helper.cli :as cli]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn check-clojure []
  (status/line :head "Checking Clojure deps")
  (shell/command {:continue true}
                 "clojure -M:outdated"))

(defn check-nodejs []
  (status/line :head "Checking Node.js deps")
  (when (not (.exists (io/file "node_modules")))
    (status/line :detail "node_modules, not found, installing.")
    (shell/command "npm ci"))

  (let [{:keys [:exit]} (shell/command {:continue true}
                                       "npm outdated")]
    (when (zero? exit)
      (status/line :detail "All Node.js dependencies seem up to date.")
      (status/line :detail "(warning: deps are only checked against installed ./node_modules)"))))

(defn task
  {:org.babashka/cli cli/base-opts}
  [_opts]
  (check-clojure)
  (check-nodejs))
