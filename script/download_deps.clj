(ns download-deps
  (:require
   [clojure.edn :as edn]
   [clojure.tools.build.api :as b]
   [lread.status-line :as status]))

(defn task [_opts]
  (status/line :head "Downloading deps")
  (let [aliases (->> "deps.edn"
                     slurp
                     edn/read-string
                     :aliases
                     keys)]
    ;; one at a time because aliases with :replace-deps will... well... you know.
    (println "Bring down default deps")
    (b/create-basis {})
    (doseq [a (sort aliases)]
      (println "Bring down deps for alias" a)
      (b/create-basis {:aliases [a]}))))
