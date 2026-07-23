(ns apply-import-vars
  (:require [helper.cli :as cli]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- apply-import-vars
  [cmd]
  (status/line :head (str "Running apply import vars " cmd))
  (shell/command "clojure -X:apply-import-vars:script" cmd))

(defn gen-code
  {:org.babashka/cli cli/base-opts}
  [_opts]
  (apply-import-vars "gen-code"))

(defn check
  {:org.babashka/cli cli/base-opts}
  [_opts]
  (apply-import-vars "check"))

(defn task
  {:org.babashka/cli
   (merge cli/base-opts
          {:cmd {"check"    {:exec-fn #'check    :doc "Generate API sources from templates"}
                 "gen-code" {:exec-fn #'gen-code :doc "Fail if API sources are stale as compared to templates"}}})}
  [_opts])
