(ns helper.jdk
  (:require [helper.shell :as shell]))

(defn version
  "Returns jdk version and major version with appropriate conversion. (ex 1.8 returns major of 8)"
  []
  (let [raw-version (->> (shell/command {:err :string}
                                        "java -version")
                         :err
                         (re-find #"version \"(.*)\"")
                         last)
        major-minor (->> raw-version
                         (re-find #"(\d+)(?:\.(\d+))?.*")
                         rest
                         (map #(when % (Integer/parseInt %))))]
    {:version raw-version
     :major (if (= (first major-minor) 1)
              (second major-minor)
              (first major-minor))}))
