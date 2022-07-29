(ns helper.graal
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.fs :as fs]
            [helper.os :as os]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn find-graal-prog [prog-name]
  (or (fs/on-path prog-name)
      (fs/at-path (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (fs/at-path (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

(defn find-native-image-prog []
  (find-graal-prog (if (= :win (os/get-os)) "native-image.cmd" "native-image")))

(defn find-gu-prog []
  (find-graal-prog (if (= :win (os/get-os)) "gu.cmd" "gu")))

(defn- assert-min-native-image-version [native-image-exe]
  (let [min-major 22
        version-out (->> (shell/command {:out :string} native-image-exe "--version")
                         :out)
        actual-major (->> version-out
                          (re-find #"(?i)GraalVM (Version )?(\d+)\.")
                          last
                          Long/parseLong)]
    (when (< actual-major min-major)
      (status/die 1
                  "Need a minimum major version of %d for Graal\nnative-image returned: %s"
                  min-major version-out))))

(defn find-graal-native-image []
  (status/line :head "Locate GraalVM native-image")
  (if-let [gu (find-gu-prog)]
    ;; its ok (and simpler and safer) to request an install of native-image when it is already installed
    (do (shell/command gu "install" "native-image")
        (let [native-image (or (find-native-image-prog)
                               (status/die 1 "failed to install GraalVM native-image, check your GraalVM installation"))]
          (status/line :detail (str "found: " native-image))
          (assert-min-native-image-version native-image)
          native-image))
    (status/die 1 "GraalVM native image not found nor its installer, check your GraalVM installation")))


(defn clean []
  (status/line :head "Clean")
  (fs/delete-file-recursively ".cpcache" true)
  (fs/delete-file-recursively "classes" true)

  (.mkdirs (io/file "classes"))
  (.mkdirs (io/file "target"))
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
    (println (str "- " (string/join "\n- " (fs/split-path-list classpath))))
    (println "\nDeps tree:")
    (shell/command "clojure" alias-opt "-Stree")
    classpath))

(defn run-native-image [{:keys [:graal-native-image :graal-reflection-fname
                                :target-path :target-exe :classpath :native-image-xmx
                                :entry-class]}]
  (status/line :head "Graal native-image compile AOT")
  (fs/delete-file-recursively target-exe true)
  (let [native-image-cmd (->> [graal-native-image
                               (str "-H:Path=" target-path)
                               (str "-H:Name=" target-exe)
                               "-H:+ReportExceptionStackTraces"
                               "-J-Dclojure.spec.skip-macros=true"
                               "-J-Dclojure.compiler.direct-linking=true"
                               (when graal-reflection-fname
                                 (str "-H:ReflectionConfigurationFiles=" graal-reflection-fname))
                               "-H:Log=registerResource:"
                               "-H:EnableURLProtocols=http,https,jar"
                               "--verbose"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.sampled.spi.AudioFileReader"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.midi.spi.MidiFileReader"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.sampled.spi.MixerProvider"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.sampled.spi.FormatConversionProvider"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.sampled.spi.AudioFileWriter"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.midi.spi.MidiDeviceProvider"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.midi.spi.SoundbankReader"
                               "-H:ServiceLoaderFeatureExcludeServices=javax.sound.midi.spi.MidiFileWriter"
                               "--no-fallback"
                               "--no-server"
                               "--report-unsupported-elements-at-runtime"
                               "-cp" (str classpath java.io.File/pathSeparator "classes")
                               (str "-J-Xmx" native-image-xmx)
                               entry-class]
                              (remove nil?))]
    (apply shell/command native-image-cmd)))
