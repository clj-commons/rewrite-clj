#!/usr/bin/env bb

(ns pure-native-test
  (:require [clojure.java.io :as io]
            [helper.env :as env]
            [helper.fs :as fs]
            [helper.graal :as graal]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-test-runner [dir]
  (status/line :head "Generate test runner")
  (fs/delete-file-recursively dir true)
  (io/make-parents dir)
  (shell/command ["clojure" "-M:script:test-common"
                  "-m" "clj-graal.gen-test-runner"
                  "--dest-dir" dir "test-by-namespace"]))

(defn -main [& args]
  (main/run-argless-cmd
   args
   (fn []
     (let [native-image-xmx "6g"
           target-exe "target/rewrite-clj-test"]
       (status/line :head "Creating native image for test")
       (status/line :detail "java -version")
       (shell/command ["java" "-version"])
       (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
       (let [graal-native-image (graal/find-graal-native-image)
             test-runner-dir "target/generated/graal"]
         (graal/clean)
         (generate-test-runner test-runner-dir)
         (let [classpath (graal/compute-classpath "test-common:native-test")]
           (graal/aot-compile-sources classpath "clj-graal.test-runner")
           (graal/run-native-image {:graal-native-image graal-native-image
                                    :target-exe target-exe
                                    :classpath classpath
                                    :native-image-xmx native-image-xmx
                                    :entry-class "clj_graal.test_runner"})))
       (status/line :head "Native image built")
       (status/line :detail "built: %s, %d bytes" target-exe (.length (io/file target-exe)))
       (status/line :head "Running tests natively")
       (shell/command [target-exe]))))
  nil)

(env/when-invoked-as-script
 (apply -main *command-line-args*))
