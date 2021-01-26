#!/usr/bin/env bb

(ns clj-watch
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(env/assert-min-versions)
(status/line :info "launching kaocha watch on clojure sources")
(shell/command (concat ["clojure" "-M:test-common:kaocha" "--watch" ] *command-line-args*))
