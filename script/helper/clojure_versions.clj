(ns helper.clojure-versions
  (:require [clojure.edn :as edn]
            [wevre.natural-compare :as natural-compare]))

(defn all
  "Returns vector of maps with :version :alias :pre-release?"
  [] 
  (->> (slurp "deps.edn")
       edn/read-string
       :aliases
       keys
       (keep (fn [k]
               (when-let [[_ prefix version] (re-find #"(clojure-pre-|clojure-)(.*)" (name k))]
                 {:version version
                  :alias k
                  :pre-release? (= "clojure-pre-" prefix)})))
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
