#!/usr/bin/env bb

(ns test-doc
  (:require [babashka.fs :as fs]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-doc-tests []
  (status/line :head "Generating tests for code blocks in documents")
  (shell/command "clojure -X:test-doc-blocks gen-tests"))

(defn run-clj-doc-tests []
  (status/line :head "Running code block tests under Clojure")
  (shell/command "clojure" "-M:test-common:test-docs:kaocha"
                 "--profile" "test-docs"
                 "--no-randomize"
                 "--reporter" "documentation"))

(defn run-cljs-doc-tests []
  (status/line :head "Running code block tests under ClojureScript")
  (let [compile-opts {:warnings {:fn-deprecated false :single-segment-namespace false}}
        out-dir "target/cljsbuild/doc-tests"
        opts-fname (fs/file out-dir "doc-tests-opts.edn")]
    (fs/create-dirs out-dir)
    (spit opts-fname compile-opts)
    (shell/command "clojure" "-M:test-common:test-docs:cljs:cljs-test"
                   "--compile-opts" opts-fname
                   "--dir" "target/test-doc-blocks/test"
                   "--out" out-dir)))

(def allowed-platforms ["clj" "cljs"])

(def args-usage "Valid args: [options]

Options:
  -p, --platform PLATFORM  Test against [clj cljs all]  [default: all]
  --help                         Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [platform (get opts "--platform")]
      (if (not (some #{platform} (conj allowed-platforms "all")))
        (status/die 1 args-usage)
        (let [platforms (if (= "all" platform)
                          allowed-platforms
                          [platform])]
          (generate-doc-tests)
          (doseq [p platforms]
            (case p
              "clj" (run-clj-doc-tests)
              "cljs" (run-cljs-doc-tests))))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
