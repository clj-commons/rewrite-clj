#!/usr/bin/env bb

(ns ci-tests
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn clean []
  (doseq [dir ["target" ".cpcache .shadow-cljs"]]
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

(defn shadow-cljs-tests []
  (shell/command ["bb" "./script/shadow_cljs_test.clj"]))

(defn cljs-bootstrap-tests []
  (if (some #{(env/get-os)} '(:mac :unix))
    (shell/command ["bb" "./script/cljs_tests.clj" "--env" "planck" "--optimizations" "none"])
    (status/line :warn "skipping planck tests, they can only be run on linux and macOS")) )

(defn main[]
  (env/assert-min-versions)
  (clean)
  (lint)
  (doc-tests)
  (clojure-tests)
  (cljs-tests)
  (shadow-cljs-tests)
  (cljs-bootstrap-tests)
  nil)

(main)
