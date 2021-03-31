#!/usr/bin/env bb

(ns apply-import-vars
  (:require [helper.env :as env]
            [helper.shell :as shell]
            [helper.status :as status]))

(defn main[args]
  (env/assert-min-versions)
  (let [cmd (first args)]
    (when (not (#{"gen-code" "check"} cmd))
      (status/fatal "Usage: apply-import-vars [gen-code|check]"))
    (status/line :info (str "Running apply import vars " cmd))
    (shell/command ["clojure" "-X:apply-import-vars:script" cmd])
    nil))

(main *command-line-args*)
