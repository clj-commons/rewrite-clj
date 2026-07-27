(ns apply-import-vars
  (:require [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- apply-import-vars
  [cmd]
  (status/line :head (str "Running apply import vars " cmd))
  (shell/command "clojure -X:apply-import-vars:script" cmd))

(defn gen-code
  {:org.babashka/cli
   {:doc "Generate API sources from templates"}}
  [_opts]
  (apply-import-vars "gen-code"))

(defn check
  {:org.babashka/cli
   {:doc "Fail if API sources are stale as compared to templates"}}
  [_opts]
  (apply-import-vars "check"))
