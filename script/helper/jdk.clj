(ns helper.jdk
  (:require [helper.shell :as shell]))

(defn get-jdk-major-version
  "Returns jdk major version converting old style appropriately. (ex 1.8 returns 8)"
  []
  (let [version
        (->> (shell/command ["java" "-version"] {:err :string})
             :err
             (re-find #"version \"(\d+)\.(\d+)\.\d+.*\"")
             rest
             (map #(Integer/parseInt %)))]
    (if (= (first version) 1)
      (second version)
      (first version))))
