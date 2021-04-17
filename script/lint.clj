#!/usr/bin/env bb

(ns lint
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.env :as env]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn cache-exists? []
  (.exists (io/file ".clj-kondo/.cache")))

(defn -main[]
  (env/assert-min-versions)
  (if (not (cache-exists?))
    (status/line :head "linting and building cache")
    (status/line :head "linting"))

  (let [lint-args (if (not (cache-exists?))
                    [(-> (shell/command ["clojure" "-A:test-common:script" "-Spath"] {:out :string})
                         :out
                         string/trim)
                     "deps.edn"]
                    ["src" "test" "script" "deps.edn"])
       {:keys [:exit]} (shell/command-no-exit
                        (concat ["clojure" "-M:clj-kondo"
                                 "--lint"]
                                lint-args
                                ["--config" ".clj-kondo/ci-config.edn"]))]
    (when (not (some #{exit} '(0 2 3)))
      (status/die exit
                  "clj-kondo existed with unexpected exit code: %d" exit))
    (System/exit exit)))

(env/when-invoked-as-script
 (-main))
