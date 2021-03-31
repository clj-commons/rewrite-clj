(ns release.version
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [helper.shell :as shell]))

(defn- repo-commit-count
  "Number of commits in the repo"
  []
  (->  (shell/command ["git" "rev-list" "HEAD" "--count"] {:out :string})
       :out
       string/trim
       Long/parseLong))

(defn- dev-specified-version []
  (-> "version.edn"
      slurp
      edn/read-string))

(defn calc []
  (let [version-template (dev-specified-version)
        patch (repo-commit-count)]
    (str (:major version-template) "."
         (:minor version-template) "."
         patch
         (cond->> (:qualifier version-template)
           true (str "-")))))

