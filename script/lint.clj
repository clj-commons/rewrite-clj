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

(defn- cache-exists? []
  (fs/exists? ".clj-kondo/.cache"))

(defn- delete-cache []
  (when (cache-exists?)
    (fs/delete-tree ".clj-kondo/.cache")))

(defn- build-cache []
  (status/line :head "clj-kondo: building cache")
  (let [clj-cp (-> (shell/command ["clojure" "-A:test" "-Spath"] {:out :string}) :out string/trim)
        bb-cp (bbcp/get-classpath)]
    (shell/command ["clojure" "-M:clj-kondo"
                    "--dependencies" "--copy-configs"
                    "--lint" clj-cp bb-cp])))

(defn- lint []
  (when (not (cache-exists?))
    (build-cache))
  (status/line :head "clj-kondo: linting")
  (let [{:keys [exit]}
        (shell/command-no-exit ["clojure" "-M:clj-kondo"
                                "--lint" "src" "test" "script" "deps.edn"])]
    (if (not (some #{exit} '(0 2 3)))
      (status/die exit "clj-kondo existed with unexpected exit code: %d" exit)
      (System/exit exit))))

(def docopt-usage "Usage: lint.clj [options]

Options:
  --help           Show this screen.
  --rebuild-cache  Force rebuild of lint cache.")

(defn -main [& args]
  (env/assert-min-versions)
  (if-let [opts (-> docopt-usage docopt/parse (docopt-match/match-argv args))]
    (cond
      (get opts "--help")
      (println docopt-usage)

      (get opts "--rebuild-cache")
      (do (delete-cache) (lint))

      :else
      (lint))
    (status/die 1 docopt-usage)))

(env/when-invoked-as-script
 (apply -main *command-line-args*))
