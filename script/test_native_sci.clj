#!/usr/bin/env bb

(ns test-native-sci
  (:require [clojure.java.io :as io]
            [helper.graal :as graal]
            [helper.main :as main]
            [helper.os :as os]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn expose-api-to-sci []
  (status/line :head "Expose rewrite-clj API to sci")
  (shell/command "clojure -M:script -m sci-test-gen-publics"))

(defn generate-reflection-file [fname]
  (status/line :head "Generate reflection file for Graal native-image")
  (io/make-parents fname)
  (shell/command "clojure -M:sci-test:gen-reflection" fname)
  (status/line :detail fname))

(defn interpret-tests [exe-fname]
  (status/line :head "Interpreting tests with sci using natively compiled binary")
  (when (not (.exists (io/file exe-fname)))
    (status/die 1 "native image %s not found." exe-fname))
  (shell/command exe-fname "--file" "script/sci_test_runner.clj" "--classpath" "test"))

(def allowed-clojure-versions '("1.10" "1.11"))

(def args-usage "Valid args: [options]

Options:
  -v, --clojure-version VERSION  Test with Clojure [1.10, 1.11] [default: 1.11]
  --help                         Show this help")


(defn validate-opts [opts]
  (when (not (some #{(get opts "--clojure-version")} allowed-clojure-versions))
        (status/die 1 args-usage)))

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (validate-opts opts)
    (let [clojure-version (get opts "--clojure-version")
          native-image-xmx "6g"
          graal-reflection-fname "target/native-image/reflection.json"
          target-path "target"
          target-exe "sci-test-rewrite-clj"
          full-target-exe (str target-path "/" target-exe (when (= :win (os/get-os)) ".exe"))]
      (status/line :head "Creating native image for testing via sci")
      (status/line :detail "java -version")
      (shell/command "java -version")
      (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
      (let [graal-native-image (graal/find-graal-native-image)]
        (graal/clean)
        (expose-api-to-sci)
        (let [classpath (graal/compute-classpath (str "graal:sci-test:" clojure-version))]
          (graal/aot-compile-sources classpath "sci-test.main")
          (generate-reflection-file graal-reflection-fname)
          (graal/run-native-image {:graal-native-image graal-native-image
                                   :graal-reflection-fname graal-reflection-fname
                                   :target-path target-path
                                   :target-exe target-exe
                                   :classpath classpath
                                   :native-image-xmx native-image-xmx
                                   :entry-class "sci_test.main"})))
      (status/line :head "build done")
      (status/line :detail "built: %s, %d bytes" full-target-exe (.length (io/file full-target-exe)))
      (interpret-tests full-target-exe)))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
