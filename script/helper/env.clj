(ns helper.env
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [helper.shell :as shell]
            [lread.status-line :as status]
            [version-clj.core :as ver]))

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

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
      (status/die 1
                  "A  minimum version of Clojure %s required.\nFound version: %s"
                  min-version version))))

(defn assert-min-versions[]
  (assert-clojure-min-version))

(defmacro when-invoked-as-script
  "Runs `body` when clj was invoked from command line as a script."
  [& body]
  `(when (= *file* (System/getProperty "babashka.file"))
     ~@body))

