#!/usr/bin/env bb

(ns apply-import-vars
  (:require [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn -main[& args]
  (env/assert-min-versions)
  (let [cmd (first args)]
    (when (not (#{"gen-code" "check"} cmd))
      (status/die 1 "Usage: apply-import-vars [gen-code|check]"))
    (status/line :head (str "Running apply import vars " cmd))
    (shell/command ["clojure" "-X:apply-import-vars:script" cmd])
    nil))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
