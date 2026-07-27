(ns helper.cli
  (:require [babashka.cli :as cli]
            [lread.status-line :as status]))

(defn colorful-error
  [{:keys [msg tree dispatch prog]}]
  (status/line :error msg)
  (status/line :detail "\n%s" (cli/format-command-help {:table tree :cmds dispatch :prog prog}))
  (System/exit 1))

(def base-opts {:restrict true :restrict-args true
                :error-fn colorful-error} )
