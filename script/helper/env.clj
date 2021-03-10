(ns helper.env
  (:require [babashka.classpath :as cp]
            [clojure.edn :as edn]
            [clojure.java.shell :as cjshell]
            [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell]
         '[helper.status :as status])

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

(defn cp-for [deps]
  ;; We use clojure.java.shell here instead of helper.shell because helper.shell relies on a babashka.process
  ;; which is only available in babashka 0.2.3 and later.
  ;; At this point we have not checked for min bb version and need deps we are adding to do so.
  (let [cmd (mapv #(if (= :win (get-os))
                     (string/replace % "\"" "\\\"")
                     %)
                  ["clojure" "-Spath" "-Sdeps" (str deps)])
        {:keys [exit out err]} (apply cjshell/sh cmd)]
    (when (not (zero? exit))
      (println "stdout:" out)
      (println "stderr:" err)
      (status/fatal (str "unable to get classpath for " deps)))
    (string/trim out)))

(cp/add-classpath (cp-for '{:deps {version-clj/version-clj {:mvn/version "0.1.2"}}}))

(require '[version-clj.core :as ver])

(defn- assert-clojure-min-version
  "Asserts minimum version of Clojure version"
  []
  (let [min-version "1.10.1.697"
        version
        (->> (shell/command ["clojure" "-Sdescribe"] {:out :string})
             :out
             edn/read-string
             :version)]
    (when (< (ver/version-compare version min-version) 0)
      (status/fatal (str "A minimum version of Clojure " min-version " required.\nFound version: " version)))))


(defn- assert-babashka-min-version
  "Asserts minimum version of Babashka"
  []
  (let [min-version "0.2.3"
        version (System/getProperty "babashka.version")]
    (when (< (ver/version-compare version min-version) 0)
      (status/fatal (str "A minimum version of Babashka " min-version " required.\nFound version: " version)))))

(defn assert-min-versions[]
  (assert-babashka-min-version)
  (assert-clojure-min-version))
