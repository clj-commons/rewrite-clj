(ns build
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]
            [whitespace-linter]))

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

(def windows? (string/starts-with? (System/getProperty "os.name") "Windows"))

(defn- localize-path [p]
  (if windows?
    (string/replace p "/" "\\")
    p))

(defn- localize-regex-path [p]
  (if windows?
    (string/replace p "/" "\\\\")
    p))

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
              :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn project-lib
  "Returns project groupid/artifactid"
  [_]
  (println lib))

(defn lint-whitespace
  "Wrap camsaul's whitespace-linter to handle OS specific file separator"
  [_]
  (whitespace-linter/lint {:paths (->> [".clj-kondo/config.edn"
                                        ".codecov.yml"
                                        ".github"
                                        ".gitignore"
                                        "CHANGELOG.adoc"
                                        "CODE_OF_CONDUCT.md"
                                        "CONTRIBUTING.md"
                                        "LICENSE"
                                        "ORIGINATOR"
                                        "README.adoc"
                                        "bb.edn"
                                        "build.clj"
                                        "deps.edn"
                                        "doc"
                                        "fig.cljs.edn"
                                        "package.json"
                                        "pom.xml"
                                        "resources"
                                        "script"
                                        "src"
                                        "template"
                                        "test"
                                        "test-isolated"
                                        "tests.edn"
                                        "version.edn"]
                                       (mapv localize-path))
                           :include-patterns ["\\.clj.?$" "\\.edn$" "\\.yaml$" "\\.adoc$" "\\.md$"
                                              "CODEOWNERS$" "LICENSE$" "ORIGINATOR$"
                                              ".clj-kondo.config.edn"]
                           :exclude-patterns (->> [;; exclude things generated
                                                   "doc/generated/.*$"
                                                   "src/rewrite_clj/(node|zip)\\.cljc$"
                                                   "src/rewrite_clj/node/string\\.clj$"
                                                   "src/rewrite_clj/zip/(edit|find|remove|seq)\\.clj"]
                                                  (mapv localize-regex-path))}))
