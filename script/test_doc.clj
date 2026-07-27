(ns test-doc
  (:require [babashka.fs :as fs]
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

(def valid-platforms ["clj" "cljs"])

(def cli-valid-platforms (conj valid-platforms "all"))

(defn task
  {:org.babashka/cli
   {:spec {:platform {:alias :p
                      :coerce :string
                      :desc "Test against"
                      :enum cli-valid-platforms
                      :default (last cli-valid-platforms)}}}}
  [{:keys [platform]}]
  (let [platforms (if (= "all" platform)
                    valid-platforms
                    [platform])]
    (generate-doc-tests)
    (doseq [p platforms]
      (case p
        "clj" (run-clj-doc-tests)
        "cljs" (run-cljs-doc-tests)))))
