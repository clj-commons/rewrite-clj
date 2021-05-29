#!/usr/bin/env bb

(ns test-cljs
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.fs :as fs]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def valid-envs '("node" "chrome-headless" "planck"))
(def valid-optimizations '("none" "advanced"))
(def valid-granularities '("all" "namespace"))

(defn compile-opts [out-dir {:keys [:env :optimizations]}]
  {:warnings {:fn-deprecated false}
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
  (-> (shell/command {:out :string}
                     "clojure"
                     "-M:test-common:script"
                     "-m" "code-info.ns-lister" "--lang" "cljs"
                     "find-all-namespaces")
      :out
      (string/split #" ")))

(defn run-tests [{:keys [:env :optimizations :run-granularity] :as opts}]
  (status/line :head "testing ClojureScript source under %s, cljs optimizations: %s" env optimizations)
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
      "all" (apply shell/command cmd)
      ;; I sometimes use namespace granularity to figure out which tests are affecting graal testing
      "namespace" (do
                    (status/line :head "+ one run for each namespace")
                    (let [nses (find-test-namespaces)
                          total-nses (count nses)]
                      (doall (map-indexed
                              (fn [ndx ns]
                                (status/line :head "%d of %d) running tests for namespace: %s" (inc ndx) total-nses ns)
                                (apply shell/command (concat cmd ["--namespace" ns])))
                              nses)))))))

(defn valid-opts? [opts]
 (and (some #{(get opts "--env")} valid-envs)
      (some #{(get opts "--optimizations")} valid-optimizations)
      (some #{(get opts "--run-granularity")} valid-granularities)))

(def args-usage "Valid args: [options]

Options:
  -e, --env ENV                      JavaScript Environment [node, chrome-headless, planck] [default: node]
  -o, --optimizations OPTIMIZATIONS  ClojureScript Optimizations [none, advanced] [default: none]
  -g, --run-granularity GRANULARITY  Run Granularity [all, namespace] [default: all]
  --help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (not (valid-opts? opts))
      (status/die 1 args-usage)

      :else
      (run-tests {:env (get opts "--env")
                  :optimizations (get opts "--optimizations")
                  :run-granularity (get opts "--run-granularity")})))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
