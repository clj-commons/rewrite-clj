(ns helper.shell
  (:require [babashka.process :as process]
            [clojure.pprint :as pprint]
            [helper.status :as status]))

(defn command-no-exit
  "Thin wrapper on babashka.process/process that does not exit on error."
  ([cmd] (command-no-exit cmd {}))
  ([cmd opts]
   (binding [process/*defaults* (merge process/*defaults* {:in :inherit
                                                           :out :inherit
                                                           :err :inherit})]
     @(process/process cmd opts))))

(defn command
  "Thin wrapper on babashka.process/process that prints error message and exits on error."
  ([cmd] (command cmd {}))
  ([cmd opts]
   (let [{:keys [exit] :as res} (command-no-exit cmd opts)]
     (if (not (zero? exit))
       (status/fatal (format "shell exited with %d for:\n %s"
                             exit
                             (with-out-str (pprint/pprint cmd)))
                     exit)
       res))))
