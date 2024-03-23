(ns helper.graal
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- find-graal-prog [prog-name]
  (or (fs/which prog-name)
      (fs/which (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (fs/which (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

(defn assert-min-version
  "After GraalVM 22.3.2, the Graal team moved to matching JDK version.
  For now, we check for something at least with the new JDK version scheme."
  []
  (if-let [java-exe (find-graal-prog "java")]
    (let [min-major 17
          version-out (->> (shell/command {:err :string} java-exe "-version")
                           :err)
          [actual-graal-major actual-jdk-major] (->> version-out
                                       (re-find #"(?i)GraalVM(?: CE)? (\d+).*\(build (\d+)")
                                       rest
                                       (map parse-long))]
      (cond
        (nil? actual-graal-major)
        (status/die 1
                    "Did not find GraalVM JDK, found version:\n%s"
                    version-out)

        (not= actual-graal-major actual-jdk-major)
        (status/die 1
                    "Found legacy GraalVM version (use a version that tracks JDK version instead):\n%s"
                    version-out)

        (< actual-graal-major min-major)
        (status/die 1
                    "Need a minimum major version of %d, found version:\n%s"
                    min-major version-out)))
    (status/die 1 "java not found")))

(defn find-graal-native-image
  "The Graal team now bundle native-image with Graal, there is no longer any need to install it."
  []
  (status/line :head "Locate GraalVM native-image")
  (let [native-image (or (find-graal-prog "native-image")
                         (status/die 1 "failed to to find GraalVM native-image, it should be bundled with your Graal installation"))]
    (status/line :detail (str "found: " native-image))
    native-image))

(defn clean []
  (status/line :head "Clean")
  (fs/delete-tree ".cpcache")
  (fs/delete-tree "classes")

  (fs/create-dirs "classes")
  (fs/create-dirs "target")
  (status/line :detail "all clean"))

(defn aot-compile-sources [classpath ns]
  (status/line :head "AOT compile sources")
  (shell/command "java"
                 "-Dclojure.compiler.direct-linking=true"
                 "-cp" classpath
                 "clojure.main"
                 "-e" (str "(compile '" ns ")")))

(defn compute-classpath [base-alias]
  (status/line :head "Compute classpath")
  (let [alias-opt (str "-A:" base-alias)
        classpath (-> (shell/command {:out :string} "clojure" alias-opt "-Spath")
                      :out
                      string/trim)]
    (println "\nClasspath:")
    (println (str "- " (string/join "\n- " (fs/split-paths classpath))))
    (println "\nDeps tree:")
    (shell/command "clojure" alias-opt "-Stree")
    classpath))

(defn run-native-image [{:keys [:graal-native-image :graal-reflection-fname
                                :target-path :target-exe :classpath :native-image-xmx
                                :entry-class]}]
  (status/line :head "Graal native-image compile AOT")
  (let [full-path-target-exe (str (fs/file target-path target-exe))]
    (when-let [exe (fs/which full-path-target-exe)]
      (status/line :detail "Deleting existing %s" exe)
      (fs/delete exe))
    (let [native-image-cmd (->> [graal-native-image
                                 "-o" full-path-target-exe
                                 "--enable-http"
                                 "--enable-https"
                                 "--features=clj_easy.graal_build_time.InitClojureClasses"
                                 "-H:+ReportExceptionStackTraces"
                                 "-J-Dclojure.spec.skip-macros=true"
                                 "-J-Dclojure.compiler.direct-linking=true"
                                 (when graal-reflection-fname
                                   (str "-H:ReflectionConfigurationFiles=" graal-reflection-fname))
                                 "--verbose"
                                 "--no-fallback"
                                 "--report-unsupported-elements-at-runtime"
                                 "-cp" (str classpath java.io.File/pathSeparator "classes")
                                 (str "-J-Xmx" native-image-xmx)
                                 entry-class]
                                (remove nil?))]
      (apply shell/command native-image-cmd))))
