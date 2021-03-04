#!/usr/bin/env bb

(ns libs-test
  "Test 3rd party libs against rewrite-clj head"
  (:require [babashka.classpath :as cp]
            [babashka.curl :as curl]
            [babashka.deps :as deps]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(deps/add-deps
 '{:deps {io.aviso/pretty {:mvn/version "0.1.36"}
          doric/doric {:mvn/version "0.9.0"}}})

(cp/add-classpath (.getParent (io/file *file*)))

(require '[doric.core :as doric]
         '[helper.shell :as shell]
         '[helper.status :as status]
         '[io.aviso.ansi :as ansi]
         '[release.version :as version])

(defn- install-local [version]
  (status/line :info (format "Installing rewrite-clj %s locally" version))
  (let [pom-bak-filename  "pom.xml.canary.bak"]
    (try
      (fs/copy "pom.xml" pom-bak-filename {:replace-existing true :copy-attributes true})
      (shell/command ["clojure" "-X:jar" ":version" (pr-str version)])
      (shell/command ["clojure" "-X:deploy:local"])
      (finally
        (fs/move pom-bak-filename "pom.xml" {:replace-existing true}))))
  nil)

(defn- fetch-lib-release [{:keys [target-root-dir name version release-url-fmt]}]
  (let [target (str (fs/file target-root-dir (format "%s-%s.zip" name version)))]
    (io/make-parents target)
    (io/copy
     (:body (curl/get (format release-url-fmt  version) {:as :stream}))
     (io/file target))
    (let [zip-root-dir (->> (shell/command ["unzip" "-qql" target] {:out :string})
                            :out
                            string/split-lines
                            first
                            (re-matches #" *\d+ +[\d-]+ +[\d:]+ +(.*)")
                            second)]
      (shell/command ["unzip" target "-d" target-root-dir])
      (str (fs/file target-root-dir zip-root-dir)))))

(defn- deps-tree [{:keys [home-dir]} cmd]
  (let [{:keys [out err]} (shell/command cmd {:dir home-dir
                                              :out :string
                                              :err :string})]
    (->  (format "stderr->:\n%s\nstdout->:\n%s" err out)
         (string/replace #"(rewrite-cljs|rewrite-clj|org.clojure/clojurescript|org.clojure/clojure)"
                         (-> "$1"
                             ansi/bold-yellow-bg
                             ansi/black))
         (println))))

(defn- lein-deps-tree [lib]
  (deps-tree lib ["lein" "deps" ":tree"]))

(defn- cli-deps-tree [lib]
  (deps-tree lib ["clojure" "-Stree"]))

(defn- patch-rewrite-cljc-sources [home-dir]
  (status/line :detail "Patching sources")
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
  (status/line :detail (format "Patching deps in: %s" filename))
  (shell/command ["clojure" "-X:deps-patcher" 
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
  (status/line :detail "Patching deps")
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
  (status/line :detail "Inlining deps")
  (shell/command ["lein" "inline-deps"] {:dir home-dir}))

(defn- refactor-nrepl-patch [{:keys [home-dir rewrite-clj-version]}]
  (status/line :detail "Patching deps")
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
;; zprint
;; 

(defn- zprint-patch [{:keys [home-dir rewrite-clj-version]}]
  (patch-deps {:filename (str (fs/file home-dir "project.clj"))
               :removals #{'rewrite-clj 'rewrite-cljs}
               :additions [['rewrite-clj rewrite-clj-version]]})

  (status/line :detail "Hacking lift-ns to compensate for rewrite-clj v0->v1 change in sexpr on nsmap keys")
  (status/line :detail "- note the word 'hacking' - hacked for to get test passing nly")
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
    (status/line :detail (format "here's the diff for %s" src-filename))
    (shell/command-no-exit ["git" "--no-pager" "diff" "--no-index" orig-filename src-filename])))

(defn- zprint-prep [{:keys [target-root-dir home-dir]}]
  (status/line :detail "Installing not-yet-released expectations/cljc-test")
  (let [clone-to-dir (str (fs/file target-root-dir "clojure-test"))]
    (shell/command ["git" "clone" "--branch" "enhancements"
                    "https://github.com/kkinnear/clojure-test.git" clone-to-dir])
    (run! #(shell/command % {:dir clone-to-dir})
          [["git" "reset" "--hard" "a6c3be067ab06f677d3b1703ee4092d25db2bb60"]
           ["clojure" "-M:jar"]
           ["mvn" "install:install-file" "-Dfile=expectations.jar" "-DpomFile=pom.xml"]]))
  (status/line :detail "Building uberjar for uberjar tests")
  (shell/command ["lein" "uberjar"] {:dir home-dir}))

;;
;; lib defs
;;

(def libs [{:name "antq"
            :version "0.11.2"
            :release-url-fmt "https://github.com/liquidz/antq/archive/%s.zip"
            :patch-fn antq-patch
            :show-deps-fn cli-deps-tree
            :test-cmd ["clojure" "-M:dev:test"]}
           {:name "carve"
            :version "0.0.2"
            :release-url-fmt "https://github.com/borkdude/carve/archive/v%s.zip"
            :patch-fn carve-patch
            :show-deps-fn cli-deps-tree
            :test-cmd ["clojure" "-M:test"]}
           {:name "cljfmt"
            :version "0.7.0"
            :root "cljfmt"
            :release-url-fmt "https://github.com/weavejester/cljfmt/archive/%s.zip"
            :patch-fn cljfmt-patch
            :show-deps-fn lein-deps-tree
            :test-cmd ["lein" "test"]}
           {:name "clojure-lsp"
            :version "2021.03.01-19.18.54"
            :release-url-fmt "https://github.com/clojure-lsp/clojure-lsp/archive/%s.zip"
            :patch-fn clojure-lsp-patch
            :show-deps-fn lein-deps-tree
            :test-cmd ["lein" "test"]}
           {:name "mranderson"
            :version "0.5.3"
            :release-url-fmt "https://github.com/benedekfazekas/mranderson/archive/v%s.zip"
            :patch-fn mranderson-patch
            :show-deps-fn lein-deps-tree
            :test-cmd ["lein" "test"]}
           {:name "rewrite-edn"
            :version "665f61cf273c79b44baacb0897d72c2157e27b09"
            :release-url-fmt "https://github.com/borkdude/rewrite-edn/zipball/%s"
            :patch-fn rewrite-edn-patch
            :show-deps-fn cli-deps-tree
            :test-cmd ["clojure" "-M:test"]}
           {:name "refactor-nrepl"
            :version "2.5.1"
            :release-url-fmt "https://github.com/clojure-emacs/refactor-nrepl/archive/v%s.zip"
            :patch-fn refactor-nrepl-patch
            :show-deps-fn lein-deps-tree
            :prep-fn refactor-nrepl-prep
            :test-cmd ["lein" "with-profile" "+1.10,+plugin.mranderson/config" "test"]}
           {:name "zprint"
            :version "1.1.1"
            :note "zprint src hacked to pass with rewrite-clj v1"
            :release-url-fmt "https://github.com/kkinnear/zprint/archive/%s.zip" 
            :patch-fn zprint-patch
            :show-deps-fn lein-deps-tree
            :prep-fn zprint-prep
            :test-cmd ["lein" "with-profile" "expectations" "test"]}])

(defn- header [text]
  (let [dashes (apply str (repeat 80 "-"))]
    (status/line :info (str dashes "\n"
                            text "\n"
                            dashes))))

(defn- test-lib [{:keys [name root patch-fn prep-fn show-deps-fn test-cmd] :as lib}]
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
    (when-not test-cmd
      (throw (ex-info (format "missing test-cmd for %s" name) {})))
    (status/line :info (format "%s: Running tests" name))
    (let [{:keys [exit]} (shell/command-no-exit test-cmd {:dir home-dir})]
      (status/line :detail "\n")
      (if (zero? exit)
        (status/line :detail (format "%s: TESTS PASSED" name))
        (status/line :warn (format "%s: TESTS FAILED" name)))
      (assoc lib :exit-code exit))))

(defn main [args]
  ;; no args = test all libs
  ;; or specify which libs, by name, to test (in order specified)
  (status/line :info "Testing 3rd party libs")
  (status/line :detail "Test popular 3rd party libs against current rewrite-clj.")
  (let [requested-libs (if (zero? (count args))
                         libs
                         (reduce (fn [ls a]
                                   (if-let [l (first (filter #(= a (:name %)) libs))]
                                     (conj ls l)
                                     ls))
                                 []
                                 args))
        target-root-dir "target/libs-test"
        _ (when (fs/exists? target-root-dir) (fs/delete-tree target-root-dir))
        rewrite-clj-version (str (version/calc) "-canary")]
    (status/line :detail (format  "requested libs: %s" (into [] (map :name requested-libs))))
    (install-local rewrite-clj-version)
    (let [results (doall (map #(test-lib (assoc %
                                                :target-root-dir target-root-dir
                                                :rewrite-clj-version rewrite-clj-version))
                              requested-libs))]
      (status/line :info "Summary")
      (println (doric/table [:name :version :note :exit-code] results))
      (System/exit (if (every? #(zero? (:exit-code %)) results) 0 1))))
  nil)

(main *command-line-args*)

