#!/usr/bin/env bb

(ns test-shadow-cljs
  (:require [helper.env :as env]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;; see also: shadow-cljs.edn
(def compiled-tests "target/shadow-cljs/node-test.js")

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (status/line :head "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
    (shell/command (if (= :win (env/get-os)) "npx.cmd" "npx")
                   "shadow-cljs" "compile" "test")
    (shell/command "node" compiled-tests))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
