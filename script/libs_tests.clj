#!/usr/bin/env bb

(ns libs-test
  "Test 3rd party libs against rewrite-clj head"
  (:require [babashka.classpath :as cp]
            [babashka.curl :as curl]
            [babashka.deps :as deps]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(deps/add-deps
 '{:deps {io.aviso/pretty {:mvn/version "0.1.36"}
          docopt/docopt {:git/url "https://github.com/nubank/docopt.clj" 
                         :sha "98814f559d2e50fdf10f43cbe3b7da0ca3cca423"}
          doric/doric {:mvn/version "0.9.0"}}})

(cp/add-classpath (.getParent (io/file *file*)))

(require '[docopt.core :as docopt]
         '[docopt.match :as docopt-match]
         '[doric.core :as doric]
         '[helper.shell :as shell]
         '[helper.status :as status]
         '[io.aviso.ansi :as ansi]
         '[release.version :as version])

(defn shcmd-no-exit 
  "Thin wrapper on babashka.process/process that does not exit on error."
  ([cmd] (shcmd-no-exit cmd {}))
  ([cmd opts]
   (status/line :detail (str "Running: " (string/join " " cmd)))
   (shell/command-no-exit cmd opts)))

(defn shcmd
  "Thin wrapper on babashka.process/process that prints error message and exits on error."
  ([cmd] (shcmd cmd {}))
  ([cmd opts]
   (status/line :detail (str "Running: " (string/join " " cmd)))
   (shell/command cmd opts)))

(defn- install-local [version]
  (status/line :info (format "Installing rewrite-clj %s locally" version))
  (let [pom-bak-filename  "pom.xml.canary.bak"]
    (try
      (fs/copy "pom.xml" pom-bak-filename {:replace-existing true :copy-attributes true})
      (shcmd ["clojure" "-X:jar" ":version" (pr-str version)])
      (shcmd ["clojure" "-X:deploy:local"])
      (finally
        (fs/move pom-bak-filename "pom.xml" {:replace-existing true}))))
  nil)

(defn- get-current-version
  "Get current available version of lib via GitHub API.

   Note that github API does have rate limiting... so if running this a lot some calls will fail.

   Could get from deps.edn RELEASE technique, but not all libs are on clojars.
   See: https://github.com/babashka/babashka/blob/master/examples/outdated.clj"
  [{:keys [github-release]}]
  (case  (:via github-release)
    ;; no official release
    :sha
    (-> (curl/get (format "https://api.github.com/repos/%s/git/refs/heads/master" (:repo github-release)))
        :body
        (json/parse-string true)
        :object
        :sha)

    ;; tags can work better than release - sometimes libs have release 1.2 that refs tag 1.1
    :tag
    (-> (curl/get (format "https://api.github.com/repos/%s/tags" (:repo github-release)))
        :body
        (json/parse-string true)
        first
        :name)

    ;; else via release which works better than tags sometimes due to the way tags sort
    (->  (curl/get (format "https://api.github.com/repos/%s/releases" (:repo github-release)))
         :body
         (json/parse-string true)
         first
         :tag_name)))

(defn- fetch-lib-release [{:keys [target-root-dir name version github-release]}]
  (let [target (str (fs/file target-root-dir (format "%s-%s.zip" name version)))
        download-url (if (= :sha (:via github-release))
                       (format "https://github.com/%s/zipball/%s" (:repo github-release) version)
                       (format "https://github.com/%s/archive/%s%s.zip"
                               (:repo github-release)
                               (or (:version-prefix github-release) "")
                               version))]
    (io/make-parents target)
    (io/copy
     (:body (curl/get download-url {:as :stream}))
     (io/file target))
    (let [zip-root-dir (->> (shcmd ["unzip" "-qql" target] {:out :string})
                            :out
                            string/split-lines
                            first
                            (re-matches #" *\d+ +[\d-]+ +[\d:]+ +(.*)")
                            second)]
      (shcmd ["unzip" target "-d" target-root-dir])
      (str (fs/file target-root-dir zip-root-dir)))))

(defn- print-deps [deps-out]
  (->  deps-out 
       (string/replace #"(org.clojure/clojurescript|org.clojure/clojure)"
                       (-> "$1"
                           ansi/bold-yellow-bg
                           ansi/black))
       (string/replace #"(rewrite-cljc|rewrite-cljs|rewrite-clj)"
                       (-> "$1"
                           ansi/bold-green-bg
                           ansi/black))
       (println)))

(defn- deps-tree [{:keys [home-dir]} cmd]
  (let [{:keys [out err]} (shcmd cmd {:dir home-dir
                                              :out :string
                                              :err :string})]
    (->  (format "stderr->:\n%s\nstdout->:\n%s" err out)
         print-deps)))

(defn- lein-deps-tree [lib]
  (deps-tree lib ["lein" "deps" ":tree"]))

(defn- cli-deps-tree [lib]
  (deps-tree lib ["clojure" "-Stree"]))

(defn- patch-rewrite-cljc-sources [home-dir]
  (status/line :detail "=> Patching sources")
  (doall (map (fn [f]
                (let [f (fs/file f)
                      content (slurp f)
                      new-content (string/replace content #"(\[rewrite-)cljc(\.\w+\s+:as\s+\w+\])"
                                                  "$1clj$2")]
                  (when (not= content new-content)
                    (status/line :detail (format "- patching source: %s" f))
                    (spit f new-content))))
              (fs/glob home-dir "**/*.{clj,cljc,cljs}"))))

(defn- patch-deps [{:keys [filename removals additions]}]
  (status/line :detail (format "=> Patching deps in: %s" filename))
  (shcmd ["clojure" "-X:deps-patcher" 
                  (if (string/ends-with? filename "deps.edn")
                    "update-deps-deps"
                    "update-project-deps")
                  :filename (pr-str filename)
                  :removals (str removals)
                  :additions (str additions)]))

;;
;; antq
;; 
(defn antq-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
               :removals #{'lread/rewrite-cljc}
               :additions {'rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}}})
  (patch-rewrite-cljc-sources home-dir))

;;
;; carve 
;; 
(defn carve-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
               :removals #{'borkdude/rewrite-cljc}
               :additions {'rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}}})
  (patch-rewrite-cljc-sources home-dir))

;;
;; cljfmt
;; 

(defn- cljfmt-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "project.clj"))
               :removals #{'rewrite-clj 'rewrite-cljs}
               :additions [['rewrite-clj rewrite-clj-version]
                           ['org.clojure/clojure "1.9.0"]]}))
  
;;
;; clojure-lsp
;; 
(defn- clojure-lsp-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
                  :removals #{'rewrite-clj/rewrite-clj 'cljfmt/cljfmt}
                  :additions {'rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}
                              'cljfmt/cljfmt {:mvn/version "0.7.0" :exclusions ['rewrite-cljs/rewrite-clj
                                                                                'rewrite-clj/rewrite-clj]}}}))
;;
;; mranderson
;; 
(defn- mranderson-patch [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "=> Patching deps")
  (let [p (str (fs/file home-dir "project.clj"))]
    (-> p
        slurp
        ;; done with exercising my rewrite-clj skills for now! :-)
        (string/replace #"rewrite-clj \"[0-9.]+\""
                        (format "rewrite-clj \"%s\"" rewrite-clj-version))
        (string/replace #"\[(org.clojure/tools.namespace\ \"[0-9-a-z.]+\")\]"
                        "[$1 :exclusions [org.clojure/tools.reader]]")
        (->> (spit p)))))

;;
;; refactor-nrepl
;; 

(defn- refactor-nrepl-prep [{:keys [home-dir]}]
  (status/line :detail "=> Inlining deps")
  (shcmd ["lein" "inline-deps"] {:dir home-dir}))

(defn- refactor-nrepl-patch [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "=> Patching deps")
  (let [p (str (fs/file home-dir "project.clj"))]
    (-> p
        slurp
        ;; done with exercising my rewrite-clj skills for now! :-)
        (string/replace #"rewrite-clj \"[0-9.]+\""
                        (format "rewrite-clj \"%s\"" rewrite-clj-version))
        (string/replace #"\[(cljfmt\ \"[0-9.]+\")\]"
                        "[$1 :exclusions [rewrite-clj rewrite-cljs]]")
       (->> (spit p)))))

;;
;; rewrite-edn
;; 
;; 
(defn rewrite-edn-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
               :removals #{'lread/rewrite-cljc}
               :additions {'rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}}})
  (patch-rewrite-cljc-sources home-dir))

;;
;; update-leiningen-dependencies-skill
;; 

(defn- update-leiningen-dependencies-skill-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
               :removals #{'rewrite-cljs}
               :additions {'rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}}}))
              
(defn- update-leiningen-dependencies-skill-prep [{:keys [home-dir]}]
  (status/line :detail "=> Installing node deps")
  (shcmd ["npm" "ci"] {:dir home-dir})
  (status/line :detail "=> Running tests as recommended, but this cmd always exits with 0, so consider this a prep step")
  (shcmd ["npm" "run" "test"] {:dir home-dir}))

;;
;; zprint
;; 

(defn- zprint-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "project.clj"))
               :removals #{'rewrite-clj 'rewrite-cljs}
               :additions [['rewrite-clj rewrite-clj-version]]})

  (status/line :detail "=> Hacking lift-ns to compensate for rewrite-clj v0->v1 change in sexpr on nsmap keys")
  (status/line :detail "- note the word 'hacking' - hacked for to get test passing only")
  (let [src-filename (str (fs/file home-dir "src/zprint/zutil.cljc"))
        orig-filename (str src-filename ".orig")
        content (slurp src-filename)
        find-str "(namespace (z/sexpr k))"
        replace-str "(namespace (keyword (z/string k)))"]
    (fs/copy src-filename orig-filename)
    (status/line :detail (format "- hacking %s" src-filename))
    (if-let [ndx (string/last-index-of content find-str)]
      (spit src-filename 
            (str (subs content 0 ndx)
                 replace-str
                 (subs content (+ ndx (count find-str)))))
      (throw (ex-info "hacking zprint failed" {})))
    (status/line :detail (format "-> here's the diff for %s" src-filename))
    (shcmd-no-exit ["git" "--no-pager" "diff" "--no-index" orig-filename src-filename])))

(defn- zprint-prep [{:keys [target-root-dir home-dir]}]
  (status/line :detail "=> Installing not-yet-released expectations/cljc-test")
  (let [clone-to-dir (str (fs/file target-root-dir "clojure-test"))]
    (shcmd ["git" "clone" "--branch" "enhancements"
                    "https://github.com/kkinnear/clojure-test.git" clone-to-dir])
    (run! #(shcmd % {:dir clone-to-dir})
          [["git" "reset" "--hard" "a6c3be067ab06f677d3b1703ee4092d25db2bb60"]
           ["clojure" "-M:jar"]
           ["mvn" "install:install-file" "-Dfile=expectations.jar" "-DpomFile=pom.xml"]]))

  (status/line :detail "=> Building uberjar for uberjar tests")
  (shcmd ["lein" "uberjar"] {:dir home-dir})
  
  (status/line :detail "=> Installing zprint locally for ClojureScript tests")
  (shcmd ["lein" "install"] {:dir home-dir}))

;;
;; lib defs
;;

(def libs [{:name "antq"
            :version "0.11.2"
            :platforms [:clj]
            :github-release {:repo "liquidz/antq"}
            :patch-fn antq-patch
            :show-deps-fn cli-deps-tree
            :test-cmds [["clojure" "-M:dev:test"]]}
           {:name "carve"
            :version "0.0.2"
            :platforms [:clj]
            :github-release {:repo "borkdude/carve"
                             :version-prefix "v"}
            :patch-fn carve-patch
            :show-deps-fn cli-deps-tree
            :test-cmds [["clojure" "-M:test"]]}
           {:name "cljfmt"
            :version "0.7.0"
            :platforms [:clj :cljs]
            :root "cljfmt"
            :github-release {:repo "weavejester/cljfmt"
                             :via :tag}
            :patch-fn cljfmt-patch
            :show-deps-fn lein-deps-tree
            :test-cmds [["lein" "test"]]}
           {:name "clojure-lsp"
            :platforms [:clj]
            :version "2021.03.01-19.18.54"
            :github-release {:repo "clojure-lsp/clojure-lsp" }
            :patch-fn clojure-lsp-patch
            :show-deps-fn lein-deps-tree
            :test-cmds [["lein" "test"]]}
           {:name "mranderson"
            :version "0.5.3"
            :platforms [:clj]
            :github-release {:repo "benedekfazekas/mranderson"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn mranderson-patch
            :show-deps-fn lein-deps-tree
            :test-cmds [["lein" "test"]]}
           {:name "rewrite-edn"
            :version "665f61cf273c79b44baacb0897d72c2157e27b09"
            :platforms [:clj]
            :github-release {:repo "borkdude/rewrite-edn"
                             :via :sha}
            :patch-fn rewrite-edn-patch
            :show-deps-fn cli-deps-tree
            :test-cmds [["clojure" "-M:test"]]}
           {:name "refactor-nrepl"
            :version "2.5.1"
            :platforms [:clj]
            :github-release {:repo "clojure-emacs/refactor-nrepl"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn refactor-nrepl-patch
            :show-deps-fn lein-deps-tree
            :prep-fn refactor-nrepl-prep
            :test-cmds [["lein" "with-profile" "+1.10,+plugin.mranderson/config" "test"]]}
           {:name "update-leiningen-dependencies-skill"
            :version "21c7ce794c83d6eed9c2a27e2fdd527b5da8ebb3"
            :platforms [:cljs]
            :github-release {:repo "atomist-skills/update-leiningen-dependencies-skill"
                             :via :sha}
            :patch-fn update-leiningen-dependencies-skill-patch
            :prep-fn update-leiningen-dependencies-skill-prep 
            :show-deps-fn cli-deps-tree
            :test-cmds [;; running tests a 2nd time via node to get non-zero exit code on failure
                        ["node" "tests.js"]]}
           {:name "zprint"
            :version "1.1.1"
            :platforms [:clj :cljs]
            :note "zprint src hacked to pass with rewrite-clj v1"
            :github-release {:repo "kkinnear/zprint"}
            :patch-fn zprint-patch
            :prep-fn zprint-prep
            :show-deps-fn (fn [lib]  
                             (status/line :detail "=> Deps for Clojure run:")
                             (lein-deps-tree lib)
                             (status/line :detail "=> Deps Clojurescript run:")
                             (cli-deps-tree lib))
            :test-cmds [["lein" "with-profile" "expectations" "test"]
                        ["clojure" "-M:cljs-runner"]]}])

(defn- header [text]
  (let [dashes (apply str (repeat 80 "-"))]
    (status/line :info (str dashes "\n"
                            text "\n"
                            dashes))))

(defn- test-lib [{:keys [name root patch-fn prep-fn show-deps-fn test-cmds] :as lib}]
  (header name)
  (let [home-dir (do
                   (status/line :info (format "%s: Fetching" name))
                   (fetch-lib-release lib))
        home-dir (str (fs/file home-dir (or root "")))
        lib (assoc lib :home-dir home-dir)]
    (when patch-fn
      (status/line :info (format "%s: Patching" name))
      (patch-fn lib))
    (when prep-fn
      (status/line :info (format "%s: Preparing" name))
      (prep-fn lib))
    (when (not show-deps-fn)
      (throw (ex-info (format "missing show-deps-fn for %s" name) {})))
    (status/line :info (format "%s: Deps report" name))
    (show-deps-fn lib)
    (when-not test-cmds
      (throw (ex-info (format "missing test-cmds for %s" name) {})))
    (status/line :info (format "%s: Running tests" name))
    (let [exit-codes (into [] (map-indexed (fn [ndx cmd]
                                             (let [{:keys [exit]} (shcmd-no-exit cmd {:dir home-dir})]
                                               (if (zero? exit)
                                                 (status/line :detail (format "=> %s: TESTS %d of %d PASSED\n" name (inc ndx) (count test-cmds)))
                                                 (status/line :warn (format "=> %s: TESTS %d of %d FAILED" name (inc ndx) (count test-cmds))))
                                               exit))
                                           test-cmds))]
      (assoc lib :exit-codes exit-codes))))

(defn- prep-target [target-root-dir]
  (status/line :info "Prep target")
  (status/line :detail (format "(re)creating: %s" target-root-dir))
  (when (fs/exists? target-root-dir) (fs/delete-tree target-root-dir))
  (.mkdirs (fs/file target-root-dir))
  (status/line :detail "git init-ing target to avoid polluting our project git config/hooks with any changes libs under test might effect")
  (shcmd ["git" "init"] {:dir target-root-dir}))

;;
;; cmds
;; 
(defn- report-outdated [requested-libs]
  (status/line :info "Checking for outdated libs")
  (status/line :detail (format  "Requested libs: %s" (into [] (map :name requested-libs))))
  (let [outdated-libs (->> requested-libs
                           (map #(assoc %
                                        :available-version (get-current-version %)
                                        :version (str (-> % :github-release :version-prefix) (:version %))))
                           (filter #(not= (:available-version %) (:version %))))]
    (if (seq outdated-libs)
      (-> (doric/table [:name :version :available-version] outdated-libs) println)
      (status/line :detail "=> All libs seems up to date"))))

(defn run-tests [requested-libs]
  (status/line :info "Testing 3rd party libs")
  (status/line :detail "Test popular 3rd party libs against current rewrite-clj.")
  (let [target-root-dir "target/libs-test"
        rewrite-clj-version (str (version/calc) "-canary")]
    (status/line :detail (format  "Requested libs: %s" (into [] (map :name requested-libs))))
    (install-local rewrite-clj-version)
    (prep-target target-root-dir)
    (let [results (doall (map #(test-lib (assoc %
                                                :target-root-dir target-root-dir
                                                :rewrite-clj-version rewrite-clj-version))
                              requested-libs))]
      (status/line :info "Summary")
      (println (doric/table [:name :version :platforms :note :exit-codes] results))
      (System/exit (if (->> results
                            (map :exit-codes)
                            flatten
                            (every? zero?))
                     0 1)))))

(def docopt-usage "Libs Tests

Usage:
  libs_tests.clj run [<lib-name>...]
  libs_tests.clj outdated [<lib-name>...]

Options:
  -h --help     Show this screen.
                   
Specifying no lib-names selects all libraries.")

(defn main [args]
  (if-let [opts (-> docopt-usage docopt/parse (docopt-match/match-argv args))]
    (let [lib-names (get opts "<lib-name>")
          requested-libs (if (zero? (count lib-names))
                           libs
                           (reduce (fn [ls a]
                                     (if-let [l (first (filter #(= a (:name %)) libs))]
                                       (conj ls l)
                                       ls))
                                   []
                                   lib-names))]
      (cond
        (get opts "outdated")
        (report-outdated requested-libs)

        (get opts "run")
        (run-tests requested-libs))

      nil)
    (status/fatal docopt-usage)))

(main *command-line-args*)


#_(->  (curl/get "https://api.github.com/repos/borkdude/rewrite-edn/releases")
     :body
     (json/parse-string true)
     #_#_first
     :tag_name)

#_(-> (curl/get "https://api.github.com/repos/borkdude/rewrite-edn/git/refs/heads/master")
    :body
    (json/parse-string true)
    :object
    :sha)

#_(-> (curl/get "https://api.github.com/repos/weavejester/cljfmt/tags")
    :body
    (json/parse-string true)
    first)

#_(get-current-version (nth libs 5))


#_(docopt/docopt docopt-usage
               *command-line-args*
               (fn [arg-map] arg-map))
