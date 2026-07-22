(ns helper.clojure-versions
  (:require [clojure.edn :as edn]
            [version-clj.core :as version]
            [wevre.natural-compare :as natural-compare]))

(defn all
  "Returns vector of maps with :version :alias :pre-release?"
  []
  (->> (slurp "deps.edn")
       edn/read-string
       :aliases
       (keep (fn [[k v]]
               (when-let [[_ prefix version] (re-find #"(clojure-pre-|clojure-)(.*)" (name k))]
                 (if-let [mvn-version (get-in v [:override-deps 'org.clojure/clojure :mvn/version])]
                   {:version version
                    :mvn-version mvn-version
                    :alias k
                    :pre-release? (= "clojure-pre-" prefix)
                    :min-jdk-major (if (version/newer-or-equal? mvn-version "1.13-alpha5") 17 8)}
                   (throw (ex-info (str "failed to find mvn/version for alias " k) {}))))))
       (sort-by :version natural-compare/natural-compare)
       (into [])))

(defn current-prod
  "Returns current production release"
  []
  (->> (all)
       (remove :pre-release?)
       last))

(defn for-native
  "Returns clojure versions for native image testing"
  []
  (into [(current-prod)] (filter :pre-release? (all))))

(defn lookup
  "Retunrs :version :alias :pre-release? map for `version`"
  [version]
  (or (some #(when (= version (:version %)) %) (all))
      (throw (ex-info (str "Clojure version not found: " version) {}))))
