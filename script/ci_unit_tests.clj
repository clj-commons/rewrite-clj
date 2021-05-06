#!/usr/bin/env bb

(ns ci-unit-tests
  (:require [helper.env :as env]
            [helper.fs :as fs]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn clean []
  (doseq [dir ["target" ".cpcache" ".shadow-cljs"]]
    (fs/delete-file-recursively dir true)))

(defn lint []
  (shell/command ["bb" "lint"]))

(defn check-import-vars []
  (shell/command ["bb" "apply-import-vars" "check"]))

(defn doc-tests[]
  (shell/command ["bb" "test-doc"]))

(defn clojure-tests []
  (doseq [version ["1.9" "1.10"]]
    (shell/command ["bb" "test-clj" "--clojure-version" version])) )

(defn cljs-tests []
  (doseq [env ["node" "chrome-headless"]
          opt ["none" "advanced"]]
    (shell/command ["bb" "test-cljs" "--env" env "--optimizations" opt])))

(defn shadow-cljs-tests []
  (shell/command ["bb" "test-shadow-cljs"]))

(defn cljs-bootstrap-tests []
  (if (some #{(env/get-os)} '(:mac :unix))
    (shell/command ["bb" "test-cljs" "--env" "planck" "--optimizations" "none"])
    (status/line :warn "skipping planck tests, they can only be run on linux and macOS")) )

(defn -main [& args]
  (main/run-argless-cmd
   args
   (fn []
     (env/assert-min-versions)
     (clean)
     (check-import-vars)
     (lint)
     (doc-tests)
     (clojure-tests)
     (cljs-tests)
     (shadow-cljs-tests)
     (cljs-bootstrap-tests)))
  nil)

(env/when-invoked-as-script
 (apply -main *command-line-args*))
