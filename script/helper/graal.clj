(ns helper.graal
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.env :as env]
            [helper.fs :as fs]
            [helper.jdk :as jdk]
            [helper.shell :as shell]
            [helper.status :as status]))

(defn find-graal-prog [prog-name]
  (or (fs/on-path prog-name)
      (fs/at-path (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (fs/at-path (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

(defn find-native-image-prog []
  (find-graal-prog (if (= :win (env/get-os)) "native-image.cmd" "native-image")))

(defn find-gu-prog []
  (find-graal-prog (if (= :win (env/get-os)) "gu.cmd" "gu")))

(defn find-graal-native-image []
  (status/line :info "Locate GraalVM native-image")
  (if-let [gu (find-gu-prog)]
    ;; its ok (and simpler and safer) to request an install of native-image when it is already installed
    (do (shell/command [gu "install" "native-image"])
        (let [native-image (or (find-native-image-prog)
                               (status/fatal "failed to install GraalVM native-image, check your GraalVM installation" 1))]
          (status/line :detail (str "found: " native-image))
          native-image))
    (status/fatal "GraalVM native image not found nor its installer, check your GraalVM installation" 1)))

(defn clean []
  (status/line :info "Clean")
  (fs/delete-file-recursively ".cpcache" true)
  (fs/delete-file-recursively "classes" true)

  (.mkdirs (io/file "classes"))
  (.mkdirs (io/file "target"))
  (status/line :detail "all clean"))

(defn aot-compile-sources [classpath ns]
  (status/line :info "AOT compile sources")
  (shell/command ["java"
                  "-Dclojure.compiler.direct-linking=true"
                  "-cp" classpath
                  "clojure.main"
                  "-e" (str "(compile '" ns ")")]))

(defn compute-classpath [alias jdk11-alias]
  (status/line :info "Compute classpath")
  (let [jdk-major-version (jdk/get-jdk-major-version)
        reflection-fix? (>= jdk-major-version 11)]
    (status/line :detail (str "JDK major version seems to be " jdk-major-version "; "
                              (if reflection-fix? "including" "excluding") " reflection fixes." ))
    (let [alias-opt (str "-A:" alias (when reflection-fix? (str ":" jdk11-alias)))
          classpath (-> (shell/command ["clojure" alias-opt "-Spath"] {:out :string})
                        :out
                        string/trim)]
      (println "\nClasspath:")
      (println (str "- " (string/join "\n- " (fs/split-path-list classpath))))
      (println "\nDeps tree:")
      (shell/command ["clojure" alias-opt "-Stree"])
      classpath)))

(defn run-native-image [{:keys [:graal-native-image :graal-reflection-fname
                                :target-exe :classpath :native-image-xmx
                                :entry-class]}]
  (status/line :info "Graal native-image compile AOT")
  (fs/delete-file-recursively target-exe true)
  (let [native-image-cmd (->> [graal-native-image
                               (str "-H:Name=" target-exe)
                               "-H:+ReportExceptionStackTraces"
                               "-J-Dclojure.spec.skip-macros=true"
                               "-J-Dclojure.compiler.direct-linking=true"
                               (when graal-reflection-fname
                                 (str "-H:ReflectionConfigurationFiles=" graal-reflection-fname))
                               "--initialize-at-build-time"
                               "-H:Log=registerResource:"
                               "--enable-all-security-services"
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
                              (remove nil?))
         time-cmd (let [os (env/get-os)]
                    (case os
                        :mac ["/usr/bin/time" "-l"]
                        :unix ["/usr/bin/time" "-v"]
                        (status/line :warn (str "I don't know how to get run stats (user/real/sys CPU, RAM use, etc) for a command on " os))))]

    (shell/command (concat time-cmd native-image-cmd))))
