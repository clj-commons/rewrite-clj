#!/usr/bin/env bb

(ns cljs-watch
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(env/assert-min-versions)
(status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
(shell/command ["clojure" "-M:test-common:cljs:fig-test"])
