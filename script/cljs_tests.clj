#!/usr/bin/env bb

(ns cljs-tests
  (:require [babashka.classpath :as cp]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.status :as status])

(def valid-envs '("node" "chrome-headless" "planck"))
(def valid-optimizations '("none" "advanced"))
(def valid-granularities '("all" "namespace"))

(defn enum-opt [short long desc valid-values]
  [short long (str desc " [" (string/join ", " valid-values) "]")
   :default (first valid-values)
   :validate [#(some #{%} valid-values)
              (str "Must be one of: " (string/join ", " valid-values))]])

(def cli-options
  [(enum-opt "-e" "--env ENV" "JavaScript Environment" valid-envs)
   (enum-opt "-o" "--optimizations OPTIMIZATIONS" "ClojureScript Optimizations" valid-optimizations)
   (enum-opt "-g" "--run-granularity GRANULARITY" "Run Granularity" valid-granularities)
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Usage: cljs_test.clj <options>"
        options-summary]
       (string/join "\n")))

(defn error-msg [errors]
  (string/join "\n" errors))

(defn validate-args [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :exit-code 0}

      errors
      {:exit-message (error-msg errors) :exit-code 1}

      :else
      {:options options})))

(defn exit [code msg]
  (if (zero? code)
    (status/line :detail msg)
    (status/line :error msg))
  (System/exit code))


(defn compile-opts [out-dir {:keys [:env :optimizations]}]
  {:warnings {:fn-deprecated false}
   ;; TODO: nil might be incorrect for target in other cases, :browser might be correct?
   :target (when (= "node" env) :nodejs)
   :optimizations (keyword optimizations)
   :pretty-print (= "none" optimizations)
   :output-dir (str out-dir "/out")
   :output-to (str out-dir "/compiled.js")
   :source-map (= "none" optimizations)})

(defn doo-opts [test-combo]
  {:karma
   {:config {"plugins" ["karma-spec-reporter" "karma-junit-reporter"]
             "reporters" ["spec" "junit"]
             "specReporter" {"suppressSkipped" "false"
                             "prefixes" {"success" "pass "
                                         "failure" "fail "
                                         "skipped" "skip "}}
             "junitReporter" {"outputDir" (str "target/out/test-results/cljs-" test-combo)}}}})

(defn find-test-namespaces []
  (-> (shell/command ["clojure"
                      "-M:test-common:script"
                      "-m" "code-info.ns-lister" "--lang" "cljs"
                      "find-all-namespaces"] {:out :string})
      :out
      (string/split #" ")))

(defn run-tests [{:keys [:env :optimizations :run-granularity] :as opts}]
  (status/line :info (format "testing ClojureScript source under %s, cljs optimizations: %s"
                             env
                             optimizations))
  (let [test-combo (str env "-" optimizations)
        out-dir (str "target/cljsbuild/test/" test-combo)
        compile-opts-fname (str out-dir "-cljs-opts.edn")
        doo-opts-fname (str out-dir "-doo-opts.edn")
        dep-aliases (cond-> ":test-common:cljs:cljs-test"
                      (= "planck" env) (str ":planck-test"))
        cmd (concat ["clojure"
                     (str "-M:" dep-aliases)]
                    (when (not= "planck" env)
                      ["--exclude" ":skip-for-cljs"])
                    ["--out" out-dir
                     "--env" env
                     "--compile-opts" compile-opts-fname
                     "--doo-opts" doo-opts-fname])]
    (fs/delete-file-recursively out-dir true)
    (.mkdirs (io/file out-dir))
    (spit compile-opts-fname (compile-opts out-dir opts))
    (spit doo-opts-fname (doo-opts test-combo))
    (case run-granularity
      "all" (shell/command cmd)
      ;; I sometimes use namespace granularity to figure out which tests are affecting graal testing
      "namespace" (do
                    (status/line :info "+ one run for each namespace")
                    (let [nses (find-test-namespaces)
                          total-nses (count nses)]
                      (doall (map-indexed
                              (fn [ndx ns]
                                (status/line :info (format "%d of %d) running tests for namespace: %s"
                                                           (inc ndx) total-nses ns))
                                (shell/command (concat cmd ["--namespace" ns])))
                              nses)))))))

(defn main [args]
  (env/assert-min-versions)
  (let [{:keys [options exit-message exit-code]} (validate-args args)]
    (if exit-message
      (exit exit-code exit-message)
      (run-tests options)))
  nil)

(main *command-line-args*)

