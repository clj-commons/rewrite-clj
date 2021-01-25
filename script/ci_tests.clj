#!/usr/bin/env bb

(ns ci-tests
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.fs :as fs]
         '[helper.shell :as shell])

(defn clean []
  (doseq [dir ["target" ".cpcache"]]
    (fs/delete-file-recursively dir true)))

(defn lint[]
  (shell/command ["bb" "./script/lint.clj"]))

(defn doc-tests[]
  (shell/command ["bb" "./script/doc_tests.clj"]))

(defn clojure-tests []
  (doseq [version ["1.9" "1.10"]]
    (shell/command ["bb" "./script/clj_tests.clj" "--clojure-version" version])) )

(defn cljs-tests []
  (doseq [env ["node" "chrome-headless"]
          opt ["none" "advanced"]]
    (shell/command ["bb" "./script/cljs_tests.clj" "--env" env "--optimizations" opt])))

(defn main[]
  (env/assert-min-versions)
  (clean)
  (lint)
  (doc-tests)
  (clojure-tests)
  (cljs-tests)
  nil)

(main)
