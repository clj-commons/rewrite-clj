(ns helper.shell
  (:require [babashka.tasks :as tasks]
            [clojure.pprint :as pprint]
            [lread.status-line :as status]))

(def default-opts {:error-fn
                   (fn die-on-error [{{:keys [exit cmd]} :proc}]
                               (status/die exit
                                           "shell exited with %d for: %s"
                                           exit
                                           (with-out-str (pprint/pprint cmd))))})

(defn command
  "Thin wrapper on babashka.tasks/shell that on error, prints status error message and exits.
  Automatically converts calls to clojure on Windows to call with powershell"
  [cmd & args]
  (let [[opts cmd args] (if (map? cmd)
                          [cmd (first args) (rest args)]
                          [nil cmd args])
        opts (merge opts default-opts)]
    (apply tasks/shell opts cmd args)))
