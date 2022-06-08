(ns build-util
  "Utilities common to and/or shared between bb and build.clj"
  (:require [clojure.edn :as edn]
            [rewrite-clj.zip :as z]))

(defn- version []
  (edn/read-string (slurp "version.edn")))

(defn bump-version
  "Bump :release in version.edn file while preserving any formatting and comments"
  []
  (spit "version.edn"
        (-> "version.edn"
            z/of-file
            (z/find-value z/next :release)
            z/right
            (z/edit inc)
            z/root-string)))

(defn version-string []
  (let [{:keys [major minor release qualifier]} (version)]
    (format "%s.%s.%s%s"
            major minor release (if qualifier
                                  (str "-" qualifier)
                                  ""))))
