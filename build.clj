(ns build
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'rewrite-clj/rewrite-clj)
(def version (let [version-template (-> "version.edn" slurp edn/read-string)
                   patch (b/git-count-revs nil)]
               (str (:major version-template) "."
                    (:minor version-template) "."
                    patch
                    (cond->> (:qualifier version-template)
                      true (str "-")))))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))
(def built-jar-version-file "target/built-jar-version.txt")

(defn jar
  "Build library jar file.
  Also writes built version to target/built-jar-version.txt for easy peasy pickup by any interested downstream operation.

  We use the optional :version-suffix to distinguish local installs from production releases.
  For example, when testing 3rd party libs against rewrite-clj master we use the suffix: canary. "
  [{:keys [version-suffix]}]
  (b/delete {:path class-dir})
  (b/delete {:path jar-file})
  (let [version (if version-suffix (format "%s-%s" version version-suffix)
                    version)]
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :scm {:tag (format "v%s" version)}
                  :basis basis
                  :src-dirs ["src"]})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    (spit built-jar-version-file version)))

(defn- built-version* []
  (when (not (.exists (io/file built-jar-version-file)))
    (throw (ex-info (str "Built jar version file not found: " built-jar-version-file) {})))
  (slurp built-jar-version-file))

(defn built-version
  ;; NOTE: Used by release script and github workflow
  "Spit out version of jar built (with no trailing newline).
  A separate task because I don't know what build.tools might spit to stdout."
  [_]
  (print (built-version*))
  (flush))

(defn install
  "Install built jar to local maven repo"
  [_]
  (b/install {:class-dir class-dir
              :lib lib
              :version (built-version*)
              :basis basis
              :jar-file jar-file}))

(defn deploy
  "Deploy built jar to clojars"
  [_]
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path lib :class-dir class-dir)}))

(defn project-lib
  "Returns project groupid/artifactid"
  [_]
  (println lib))
