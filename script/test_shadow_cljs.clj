#!/usr/bin/env bb

(ns test-shadow-cljs
  (:require [helper.jdk :as jdk]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;; see also: shadow-cljs.edn
(def compiled-tests "target/shadow-cljs/node-test.js")

(defn -main [& args]
  (let [{:keys [version major]} (jdk/version)]
    (when (<= major 8)
      (status/die 1 "shadow-cljs requires JDK 11 or above, found version %s" version)))
  (when (main/doc-arg-opt args)
    (status/line :head "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
    (shell/command "npx" "shadow-cljs" "compile" "test")
    (shell/command "node" compiled-tests))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
