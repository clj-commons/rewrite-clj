#!/usr/bin/env bb

(ns test-libs
  "Test 3rd party libs against rewrite-clj head"
  (:require [babashka.curl :as curl]
            [babashka.fs :as fs]
            [build-shared]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.deps-patcher :as deps-patcher]
            [helper.main :as main]
            [helper.shell :as shell]
            [io.aviso.ansi :as ansi]
            [lread.status-line :as status]))

(defn shcmd [cmd & args]
  (let [[opts cmd args] (if (map? cmd)
                          [cmd (first args) (rest args)]
                          [{} cmd args])]
    (status/line :detail (str "Running: " cmd " " (string/join " " args)))
    (apply shell/command opts cmd args)))

(defn- install-local [canary-version]
  (status/line :head "Installing canary rewrite-clj locally")
  (shcmd "clojure -T:build install :version-override" (pr-str canary-version)))

(defn- get-current-version
  "Get current available version of lib via GitHub API.

   Note that github API does have rate limiting... so if running this a lot some calls will fail.
   Set GITHUB_TOKEN env var to a GitHub personal access token to increase this limit.
   The token needs no scopes.

   Could get from deps.edn RELEASE technique, but not all libs are on clojars.
   See: https://github.com/babashka/babashka/blob/master/examples/outdated.clj"
  [{:keys [github-release]}]
  (let [token (System/getenv "GITHUB_TOKEN")
        opts (if token
               {:headers {"Authorization" (format "Bearer %s" token)}}
               {})]
    (case (:via github-release)
      ;; no official release
      :sha
      (-> (curl/get (format "https://api.github.com/repos/%s/git/refs/heads/master" (:repo github-release))
                    opts)
          :body
          (json/parse-string true)
          :object
          :sha)

      ;; tags can work better than release - sometimes libs have release 1.2 that refs tag 1.1
      :tag
      (-> (curl/get (format "https://api.github.com/repos/%s/tags" (:repo github-release))
                    opts)
          :body
          (json/parse-string true)
          first
          :name)

      ;; else via release which works better than tags sometimes due to the way tags sort
      (->  (curl/get (format "https://api.github.com/repos/%s/releases" (:repo github-release))
                     opts)
           :body
           (json/parse-string true)
           first
           :tag_name))))

(defn- fetch-lib-release [{:keys [target-root-dir name version github-release]}]
  (let [target (str (fs/file target-root-dir (format "%s-%s.zip" name version)))
        download-url (if (= :sha (:via github-release))
                       (format "https://github.com/%s/zipball/%s" (:repo github-release) version)
                       (format "https://github.com/%s/archive/%s%s.zip"
                               (:repo github-release)
                               (or (:version-prefix github-release) "")
                               version))]
    (status/line :detail "Downloading lib release from: %s" download-url)
    (io/make-parents target)
    (io/copy
     (:body (curl/get download-url {:as :stream}))
     (io/file target))
    (let [zip-root-dir (->> (shcmd {:out :string} "unzip -qql" target)
                            :out
                            string/split-lines
                            first
                            (re-matches #" *\d+ +[\d-]+ +[\d:]+ +(.*)")
                            second)]
      (shcmd "unzip" target "-d" target-root-dir)
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
  (let [{:keys [out err]} (shcmd {:dir home-dir
                                  :out :string
                                  :err :string}
                                 cmd)]
    (->  (format "stderr->:\n%s\nstdout->:\n%s" err out)
         print-deps)))

(defn- lein-deps-tree [lib]
  (deps-tree lib "lein deps :tree"))

(defn- cli-deps-tree [lib]
  (deps-tree lib "clojure -Stree"))

(defn- patch-deps [{:keys [filename] :as opts}]
  (status/line :detail "=> Patching deps in: %s" filename)
  (if (string/ends-with? filename "deps.edn")
    (deps-patcher/update-deps-deps opts)
    (deps-patcher/update-project-deps opts)))

;;
;; Generic patch for deps.edn rewrite-clj v1 projects to v1 current
;;
(defn deps-edn-v1-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "deps.edn"))
               :removals #{'rewrite-clj 'rewrite-clj/rewrite-clj}
               :additions [['rewrite-clj/rewrite-clj {:mvn/version rewrite-clj-version}]]}))

(defn- project-clj-v1-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "project.clj"))
               :removals #{'rewrite-clj 'rewrite-clj/rewrite-clj}
               :additions [['rewrite-clj/rewrite-clj rewrite-clj-version]]}))

(defn- replace-in-file [fname match replacement]
  (let [orig-filename (str fname ".orig")
        content (slurp fname)]
    (fs/copy fname orig-filename)
    (status/line :detail "- hacking %s" fname)
    (let [new-content (string/replace content match replacement)]
      (if (= new-content content)
        (throw (ex-info (format "hacking file failed: %s" fname) {}))
        (spit fname new-content)))))

(defn- show-patch-diff [{:keys [home-dir]}]
  (let [{:keys [exit]} (shcmd {:dir home-dir :continue true}
                              "git --no-pager diff --exit-code")]
    (when (zero? exit)

      (status/die 1 "found no changes, patch must have failed" ))))

;;
;; ancient-clj
;;
(defn ancient-clj-patch [{:keys [home-dir rewrite-clj-version]}]
 (patch-deps {:filename (str (fs/file home-dir "project.clj"))
              ;; we remove and add tools.reader because project.clj has pedantic? :abort enabled
              :removals #{'rewrite-clj 'org.clojure/tools.reader}
              :additions [['org.clojure/tools.reader "1.5.2"]
                          ['rewrite-clj rewrite-clj-version]]}))

;;
;; clojure-lsp
;;
;; Has moved the deps.edn we care about into a lib subdir
;;
(defn clojure-lsp-patch [lib-opts]
  (deps-edn-v1-patch (update lib-opts :home-dir #(str (fs/file % "lib")))))

(defn clojure-lsp-deps [lib-opts]
  (cli-deps-tree (update lib-opts :home-dir #(str (fs/file % "lib")))))

;;
;; depot
;;
(defn depot-patch [{:keys [home-dir] :as lib}]
  (deps-edn-v1-patch lib)
  (status/line :detail "=> depot uses but does not require rewrite-clj.node, need to adjust for rewrite-clj v1")
  (replace-in-file (str (fs/file home-dir "src/depot/zip.clj"))
                   "[rewrite-clj.zip :as rzip]"
                   "[rewrite-clj.zip :as rzip] [rewrite-clj.node]"))

;;
;; lein ancient
;;
(defn- lein-ancient-patch [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "=> Patching deps")
  (let [p (str (fs/file home-dir "project.clj"))]
    (-> p
        slurp
        ;; done with exercising my rewrite-clj skills for now! :-)
        (string/replace #"rewrite-clj \"(\d+\.)+.*\""
                        (format "rewrite-clj \"%s\"" rewrite-clj-version))
        (string/replace #"org.clojure/tools.reader \"(\d+\.)+.*\""
                        "org.clojure/tools.reader \"1.5.2\"")
        (->> (spit p)))))

;;
;; mranderson
;;
(defn- mranderson-patch [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "=> Patching deps")
  (let [p (str (fs/file home-dir "project.clj"))]
    (-> p
        slurp
        ;; done with exercising my rewrite-clj skills for now! :-)
        (string/replace #"rewrite-clj \"(\d+\.)+.*\""
                        (format "rewrite-clj \"%s\"" rewrite-clj-version))
        (string/replace #"\[(org.clojure/tools.namespace\ \"(\d+\.)+.*\")\]"
                        "[$1 :exclusions [org.clojure/tools.reader]]")
        (->> (spit p)))))

;;
;; refactor-nrepl
;;
(defn- refactor-nrepl-patch
  "custom because my generic does not handle ^:inline-dep syntax"
  [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "=> Patching deps")
  (let [p (str (fs/file home-dir "project.clj"))]
    (-> p
        slurp
        ;; done with exercising my rewrite-clj skills for now! :-)
        (string/replace #"rewrite-clj \"(\d+\.)+.*\""
                        (format "rewrite-clj \"%s\"" rewrite-clj-version))
        ;; pedantic is enabled for CI, so adjust to match rewrite-clj so we don't fail
        (string/replace #"org.clojure/tools.reader \"(\d+\.)+.*\""
                        "org.clojure/tools.reader \"1.5.2\"")
        (->> (spit p)))))

;;
;; zprint
;;

(defn- zprint-patch [{:keys [home-dir] :as lib}]
  ;; zprint uses both deps.edn and project.clj, patch them both
  (deps-edn-v1-patch lib)
  (project-clj-v1-patch lib)
  ;; zprint 1.3.0 has a single failing test (at least on linux).
  ;; See https://github.com/kkinnear/zprint/issues/355
  ;; Disable the failing test which starts on line 211
  (status/line :detail "Patching for failing test in v1.3.0")
  (let [p (str (fs/file home-dir "test/zprint/main_test.cljc"))
        lines (-> p slurp string/split-lines)
        new-lines (update lines 210 #(str "#_" %))]
    (->> (string/join "\n" new-lines)
         (spit p))))

(defn- zprint-prep [{:keys [home-dir]}]
  (status/line :detail "=> Building uberjar for uberjar tests")
  (shcmd {:dir home-dir} "lein uberjar"))

;;
;; lib defs
;;

(def libs [{:name "adorn"
            :version "0.1.131-alpha"
            :platforms [:clj :cljs]
            :github-release {:repo "fabricate-site/adorn"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            ;; TODO: cljs tests were spitting out lots of warnings and errors when I tried,
            ;; revisit next version bump
            :test-cmds ["clojure -X:dev:test"]}
           {:name "ancient-clj"
            :version "2.0.0"
            :platforms [:clj]
            :github-release {:repo "xsc/ancient-clj"
                             :version-prefix "v"}
            :patch-fn ancient-clj-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein kaocha"]}
           {:name "antq"
            :version "2.11.1276"
            :platforms [:clj]
            :github-release {:repo "liquidz/antq"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -M:dev:test"]}
           {:name "carve"
            :version "0.3.5"
            :platforms [:clj]
            :github-release {:repo "borkdude/carve"
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -M:test"]}
           {:name "clerk"
            :version "0.17.1102"
            :platforms [:clj]
            :github-release {:repo "nextjournal/clerk"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["bb test:clj :kaocha/reporter '[kaocha.report/documentation]'"]}
           {:name "clj-mergetool"
            :version "0.7.0"
            :platforms [:clj]
            :github-release {:repo "kurtharriger/clj-mergetool"
                             :via :tag}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -T:build ci"]}
           {:name "cljfmt"
            :version "0.13.1"
            :platforms [:clj :cljs]
            :root "cljfmt"
            :github-release {:repo "weavejester/cljfmt"
                             :via :tag}
            :patch-fn project-clj-v1-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test"]}
           {:name "cljstyle"
            :version "0.17.642"
            :platforms [:clj]
            :github-release {:repo "greglook/cljstyle"
                             :via :tag}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["bin/test check"
                        "bin/test unit"]}
           {:name "clojure-lsp"
            :platforms [:clj]
            :version "2025.05.27-13.56.57"
            :github-release {:repo "clojure-lsp/clojure-lsp"}
            :patch-fn clojure-lsp-patch
            :show-deps-fn clojure-lsp-deps
            :test-cmds ["bb test"]}
           {:name "depot"
            :platforms [:clj]
            :note "1 patch required due to using, but not requiring, rewrite-clj.node"
            :version "2.2.0"
            :github-release {:repo "Olical/depot"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn depot-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["bin/kaocha --reporter documentation"]}
           {:name "kibit"
            :platforms [:clj]
            :version "0.1.11"
            :github-release {:repo "clj-commons/kibit"}
            :patch-fn project-clj-v1-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test-all"]}
           {:name "kusonga"
            :platforms [:clj]
            :version "0.1.2"
            :github-release {:repo "FiV0/kusonga"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -X:test"]}
           {:name "lein-ancient"
            :platforms [:clj]
            :version "1.0.0-RC3"
            :github-release {:repo "xsc/lein-ancient"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn lein-ancient-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test"]}
           {:name "mranderson"
            :version "0.5.3"
            :platforms [:clj]
            :github-release {:repo "benedekfazekas/mranderson"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn mranderson-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test"]}
           {:name "mutant"
            :version "0.2.0"
            :platforms [:clj]
            :note "Dormant project"
            :github-release {:repo "jstepien/mutant"
                             :via :tag}
            :patch-fn project-clj-v1-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test"]}
           {:name "reval"
            :version "0.0.38"
            :github-release {:repo "pink-gorilla/reval"
                             :version-prefix "v"
                             :via :tag}
            :root "reval"
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -M:test"]}
           {:name "rewrite-edn"
            :version "0.4.9"
            :platforms [:clj]
            :github-release {:repo "borkdude/rewrite-edn"
                             :version-prefix "v"
                             :via :tag}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -M:test"]}
           {:name "refactor-nrepl"
            :version "3.11.0"
            :platforms [:clj]
            :github-release {:repo "clojure-emacs/refactor-nrepl"
                             :via :tag
                             :version-prefix "v"}
            :patch-fn refactor-nrepl-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["make test"]}
           {:name "rich-comment-tests"
            :version "1.0.3"
            :platforms [:clj] ;; and bb but we don't test that here
            :github-release {:repo "matthewdowney/rich-comment-tests"
                             :version-prefix "v"
                             :via :tag}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["bb test-clj"]}
           {:name "splint"
            :version "1.20.0"
            :platforms [:clj]
            :github-release {:repo "NoahTheDuke/splint"
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["clojure -M:dev:test:runner"]}
           {:name "test-doc-blocks"
            :version "1.2.21"
            :platforms [:clj :cljs]
            :note "generates tests under clj, but can also be run under cljs"
            :github-release {:repo "lread/test-doc-blocks"
                             :version-prefix "v"}
            :patch-fn deps-edn-v1-patch
            :show-deps-fn cli-deps-tree
            :test-cmds ["bb test-unit"
                        "bb test-integration"
                        "bb test-gen-clj"]}
           {:name "umschreiben-clj"
            :version "0.1.0"
            :platforms [:clj]
            :github-release {:repo "nubank/umschreiben-clj"
                             :via :tag}
            :patch-fn project-clj-v1-patch
            :show-deps-fn lein-deps-tree
            :test-cmds ["lein test"]}
           {:name "zprint"
            :version "1.3.0"
            :note "1) planck cljs tests disabled for now: https://github.com/planck-repl/planck/issues/1088"
            :platforms [:clj :cljs]
            :github-release {:repo "kkinnear/zprint"}
            :patch-fn zprint-patch
            :prep-fn zprint-prep
            :show-deps-fn (fn [lib]
                            (status/line :detail "=> project.clj:")
                            (lein-deps-tree lib)
                            (status/line :detail "=> deps.edn:")
                            (cli-deps-tree lib))
            :test-cmds ["clojure -M:cljtest"
                        ;; disable zprint cljs tests for now, see https://github.com/planck-repl/planck/issues/1088
                        #_"clojure -M:cljs-runner"]
            ;; :requires ["planck"] ;; re-enable when cljs tests are re-enabled
            }])

(defn- header [text]
  (let [dashes (apply str (repeat 80 "-"))]
    (status/line :head (str dashes "\n"
                            text "\n"
                            dashes))))

(defn- test-lib [{:keys [name root patch-fn prep-fn show-deps-fn test-cmds cleanup-fn] :as lib}]
  (header name)
  (let [home-dir (do
                   (status/line :head "%s: Fetching" name)
                   (fetch-lib-release lib))
        home-dir (str (fs/file home-dir (or root "")))
        lib (assoc lib :home-dir home-dir)]
    (status/line :detail "git init-ing target, some libs expect that they were cloned")
    (shcmd {:dir home-dir} "git init")
    (status/line :detail "git adding, so that we can easily show effect of our patches later")
    (shcmd {:dir home-dir} "git add .")

    (when patch-fn
      (status/line :head "%s: Patching" name)
      (patch-fn lib))
    (try
      (when prep-fn
        (status/line :head "%s: Preparing" name)
        (prep-fn lib))
      (when (not show-deps-fn)
        (throw (ex-info (format "missing show-deps-fn for %s" name) {})))
      (status/line :head "%s: Effect of our patches" name)
      (show-patch-diff lib)
      (status/line :head "%s: Deps report" name)
      (show-deps-fn lib)
      (when-not test-cmds
        (throw (ex-info (format "missing test-cmds for %s" name) {})))
      (status/line :head "%s: Running tests" name)
      (let [exit-codes (into [] (map-indexed (fn [ndx cmd]
                                               (let [{:keys [exit]} (shcmd {:dir home-dir :continue true} cmd)]
                                                 (if (zero? exit)
                                                   (status/line :detail "=> %s: TESTS %d of %d PASSED\n" name (inc ndx) (count test-cmds))
                                                   (status/line :warn "=> %s: TESTS %d of %d FAILED" name (inc ndx) (count test-cmds)))
                                                 exit))
                                             test-cmds))]
        (assoc lib :exit-codes exit-codes))
      (finally
        (when cleanup-fn
          (status/line :head "%s: Cleanup" name)
          (cleanup-fn lib))))))

(defn- prep-target [target-root-dir]
  (status/line :head "Prep target")
  (status/line :detail "(re)creating: %s" target-root-dir)
  (when (fs/exists? target-root-dir) (fs/delete-tree target-root-dir))
  (.mkdirs (fs/file target-root-dir)))

;;
;; cmds
;;
(defn- report-outdated [requested-libs]
  (status/line :head "Checking for outdated libs")
  (status/line :detail "Requested libs: %s" (into [] (map :name requested-libs)))
  (let [outdated-libs (->> requested-libs
                           (map #(assoc %
                                        :available-version (get-current-version %)
                                        :version (str (-> % :github-release :version-prefix) (:version %))))
                           (filter #(not= (:available-version %) (:version %))))]
    (if (seq outdated-libs)
      (-> (doric/table [:name :version :available-version :note] outdated-libs) println)
      (status/line :detail "=> All libs seems up to date"))))

(defn- print-results [results]
  (status/line :head "Summary")
  (println (doric/table [:name :version :platforms :exit-codes] results))
  (when (seq (filter :note results))
    (status/line :detail "Notes")
    (println (doric/table [:name :note] (filter :note results)))))

(defn run-tests [requested-libs]
  (status/line :head "Testing 3rd party libs")
  (status/line :detail "Test popular 3rd party libs against current rewrite-clj.")
  (let [target-root-dir "target/test-libs"]
    (status/line :detail "Requested libs: %s" (into [] (map :name requested-libs)))
    (let [canary-version (str (build-shared/lib-version) "-canary")]
      (install-local canary-version)
      (prep-target target-root-dir)
      (let [results (doall (map #(test-lib (assoc %
                                                  :target-root-dir target-root-dir
                                                  :rewrite-clj-version canary-version))
                                requested-libs))]
        (print-results results)
        (System/exit (if (->> results
                              (map :exit-codes)
                              flatten
                              (every? zero?))
                       0 1))))))

(def args-usage "Valid args:
  list [--format=json]
  run [<lib-name>...]
  outdated [<lib-name>...]
  --help

Commands:
  list     List libs we can test against
  run      Run tests for specified libs
  outdated Check specified libs for newer versions

Options:
  --help  Show this help

Specifying no lib-names selects all libraries.")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "list")
      (if (= "json" (get opts "--format"))
        (status/line :detail (->> libs
                                  (map (fn [{:keys [name requires]}]
                                         {:lib-name name
                                          :requires (or requires [])}))
                                  json/generate-string))
        (status/line :detail (str "available libs: " (string/join " " (map :name libs)))))

      :else
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
          (not (seq requested-libs))
          (status/die 1 "no specified lib-names found")

          (get opts "outdated")
          (report-outdated requested-libs)

          (get opts "run")
          (run-tests requested-libs))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
