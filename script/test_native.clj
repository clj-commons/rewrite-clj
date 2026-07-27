(ns test-native
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [helper.clojure-versions :as clojure-versions]
            [helper.graal :as graal]
            [helper.os :as os]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn generate-test-runner [dir]
  (status/line :head "Generate test runner")
  (fs/delete-tree dir)
  (io/make-parents dir)
  (shell/command "clojure" "-M:script:test-common"
                 "-m" "clj-graal.gen-test-runner"
                 "--dest-dir" dir "test-by-namespace"))

(def cli-clojure-versions (mapv :version (clojure-versions/for-native)))

(defn task
  {:org.babashka/cli {:spec (clojure-versions/cli-opt cli-clojure-versions)}}
  [{:keys [clojure-version]}]
  (graal/assert-min-version)
  (let [clojure-version (clojure-versions/lookup clojure-version)
        native-image-xmx "6g"
        target-path "target"
        target-exe "rewrite-clj-test"
        full-target-exe (str target-path "/" target-exe (when (= :win (os/get-os)) ".exe"))]
    (status/line :head "Creating native image for test")
    (status/line :detail "java -version")
    (shell/command "java -version")
    (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
    (let [graal-native-image (graal/find-graal-native-image)
          test-runner-dir "target/generated/graal"]
      (graal/clean)
      (generate-test-runner test-runner-dir)
      (let [classpath (graal/compute-classpath (str "test-common:graal:native-test:" (:alias clojure-version)))]
        (graal/aot-compile-sources classpath "clj-graal.test-runner")
        (graal/run-native-image {:graal-native-image graal-native-image
                                 :target-path target-path
                                 :target-exe target-exe
                                 :classpath classpath
                                 :native-image-xmx native-image-xmx
                                 :entry-class "clj_graal.test_runner"})))
    (status/line :head "Native image built")
    (status/line :detail "built: %s, %d bytes" full-target-exe (.length (io/file full-target-exe)))
    (status/line :head "Running tests natively")
    (shell/command full-target-exe)))
