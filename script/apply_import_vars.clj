#!/usr/bin/env bb

(ns apply-import-vars
  (:require [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def args-usage "Valid args: (gen-code|check|--help)

Commands:
  gen-code Generate API sources from templates
  check    Fail if API sources are stale as compared to templates

Options:
  --help   Show this help")

(defn -main[& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [cmd (if (get opts "check") "check" "gen-code")]
      (status/line :head (str "Running apply import vars " cmd))
      (shell/command "clojure -X:apply-import-vars:script" cmd)))
    nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
