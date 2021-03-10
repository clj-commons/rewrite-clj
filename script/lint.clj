#!/usr/bin/env bb

(ns lint
  (:require [babashka.classpath :as cp]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn cache-exists? []
  (.exists (io/file ".clj-kondo/.cache")))

(defn lint[]
  (env/assert-min-versions)
  (if (not (cache-exists?))
    (status/line :info "linting and building cache")
    (status/line :info "linting"))

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
      (status/fatal (str "clj-kondo existed with unexpected exit code: " exit) exit))
    (System/exit exit)))

(lint)
