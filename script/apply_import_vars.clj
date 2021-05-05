#!/usr/bin/env bb

(ns apply-import-vars
  (:require [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def arg-usage (str "Valid args: (gen-code|check|--help)\n"
                    "\n"
                    " gen-code Generate API sources from templates\n"
                    " check    Fail if API sources are stale as compared to templates\n"
                    " --help   Show this help"))

(defn -main[& args]
  (env/assert-min-versions)
  (let [cmd (first args)]
    (cond
      (= "--help" cmd)
      (status/line :detail arg-usage)

      (not (#{"gen-code" "check"} cmd))
      (status/die 1 arg-usage)

      :else
      (do (status/line :head (str "Running apply import vars " cmd))
          (shell/command ["clojure" "-X:apply-import-vars:script" cmd])))
    nil))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
