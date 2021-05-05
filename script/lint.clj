#!/usr/bin/env bb

(ns lint
  (:require [babashka.classpath :as bbcp]
            [babashka.fs :as fs]
            [clojure.string :as string]
            [docopt.core :as docopt]
            [docopt.match :as docopt-match]
            [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def clj-kondo-cache ".clj-kondo/.cache")

(defn- cache-exists? []
  (fs/exists? clj-kondo-cache))

(defn- delete-cache []
  (when (cache-exists?)
    (fs/delete-tree clj-kondo-cache)))

(defn- build-cache []
  (status/line :head "clj-kondo: building cache")
  (let [clj-cp (-> (shell/command ["clojure" "-A:test" "-Spath"] {:out :string}) :out string/trim)
        bb-cp (bbcp/get-classpath)]
    (shell/command ["clojure" "-M:clj-kondo"
                    "--dependencies" "--copy-configs"
                    "--lint" clj-cp bb-cp])))

(defn- lint []
  (if (not (cache-exists?))
    (build-cache)
    (let [updated-dep-files (fs/modified-since clj-kondo-cache ["deps.edn" "bb.edn"])]
      (when (seq updated-dep-files)
        (status/line :detail "Found deps files newer than lint cache: %s" (mapv str updated-dep-files))
        (delete-cache)
        (build-cache))))
  (status/line :head "clj-kondo: linting")
  (let [{:keys [exit]}
        (shell/command-no-exit ["clojure" "-M:clj-kondo"
                                "--lint" "src" "test" "script" "deps.edn"])]
    (cond
      (= 2 exit) (status/die exit "clj-kondo found one or more lint errors")
      (= 3 exit) (status/die exit "clj-kondo found one or more lint warnings")
      (> exit 0) (status/die exit "clj-kondo returned unexpected exit code"))))

(def docopt-usage "Valid args: [options]

Options:
  --help           Show this screen.
  --rebuild-cache  Force rebuild of lint cache.")

(defn -main [& args]
  (env/assert-min-versions)
  (if-let [opts (-> docopt-usage
                    (string/replace-first "Valid args:" "Usage: foo") ;; conform to what docopt expects
                    docopt/parse
                    (docopt-match/match-argv args))]
    (cond
      (get opts "--help")
      (status/line :detail docopt-usage)

      (get opts "--rebuild-cache")
      (do (delete-cache) (lint))

      :else
      (lint))
    (status/die 1 docopt-usage)))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
