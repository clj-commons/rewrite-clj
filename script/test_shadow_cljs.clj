#!/usr/bin/env bb

(ns test-shadow-cljs
  (:require [helper.main :as main]
            [helper.os :as os]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;; see also: shadow-cljs.edn
(def compiled-tests "target/shadow-cljs/node-test.js")

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (status/line :head "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
    (shell/command (if (= :win (os/get-os)) "npx.cmd" "npx")
                   "shadow-cljs" "compile" "test")
    (shell/command "node" compiled-tests))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
