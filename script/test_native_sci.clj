#!/usr/bin/env bb

(ns test-native-sci
  (:require [clojure.java.io :as io]
            [helper.env :as env]
            [helper.graal :as graal]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn expose-api-to-sci []
  (status/line :head "Expose rewrite-clj API to sci")
  (shell/command ["clojure" "-M:script" "-m" "sci-test-gen-publics"]))

(defn generate-reflection-file [fname]
  (status/line :head "Generate reflection file for Graal native-image")
  (io/make-parents fname)
  (shell/command ["clojure" "-M:sci-test:gen-reflection" fname])
  (status/line :detail fname))

(defn interpret-tests []
  (status/line :head "Interpreting tests with sci using natively compiled binary")
  (let [exe-fname (if (= :win (env/get-os))
                    "target/sci-test-rewrite-clj.exe"
                    "target/sci-test-rewrite-clj")]
    (when (not (.exists (io/file exe-fname)))
      (status/die 1 "native image %s not found." exe-fname))
    (shell/command [exe-fname "--file" "script/sci_test_runner.clj" "--classpath" "test"])))

(defn -main [& args]
  (main/run-argless-cmd
   args
   (fn []
     (let [native-image-xmx "6g"
           graal-reflection-fname "target/native-image/reflection.json"
           target-exe "target/sci-test-rewrite-clj"]
       (status/line :head "Creating native image for testing via sci")
       (status/line :detail "java -version")
       (shell/command ["java" "-version"])
       (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
       (let [graal-native-image (graal/find-graal-native-image)]
         (graal/clean)
         (expose-api-to-sci)
         (let [classpath (graal/compute-classpath "sci-test")]
           (graal/aot-compile-sources classpath "sci-test.main")
           (generate-reflection-file graal-reflection-fname)
           (graal/run-native-image {:graal-native-image graal-native-image
                                    :graal-reflection-fname graal-reflection-fname
                                    :target-exe target-exe
                                    :classpath classpath
                                    :native-image-xmx native-image-xmx
                                    :entry-class "sci_test.main"})))
       (status/line :head "build done")
       (status/line :detail "built: %s, %d bytes" target-exe (.length (io/file target-exe)))
       (interpret-tests))))
  nil)

(env/when-invoked-as-script
 (apply -main *command-line-args*))
