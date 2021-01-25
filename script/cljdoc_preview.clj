#!/usr/bin/env bb

(ns cljdoc-docker-preview
  (:require [babashka.classpath :as cp]
            [babashka.curl :as curl]
            [clojure.java.browse :as browse]
            [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.status :as status])

;;
;; constants
;;

(def cljdoc-root-temp-dir "/tmp/cljdoc-preview")
(def cljdoc-db-dir (str cljdoc-root-temp-dir  "/db"))
(def cljdoc-container {:name "cljdoc-server"
                       :image "cljdoc/cljdoc"
                       :port 8000})

;;
;; Prerequisites
;;

(defn check-prerequisites []
  (let [missing-cmds (doall (remove #(fs/on-path %) ["mvn" "git" "docker"]))]
    (when (seq missing-cmds)
      (status/fatal (string/join "\n" ["Required commands not found:"
                                       (string/join "\n" missing-cmds)])))))
;;
;; os/fs support
;;
(defn cwd[]
  (System/getProperty "user.dir"))

(defn home-dir[]
  (System/getProperty "user.home"))

;;
;; maven/pom
;;
(defn get-from-pom [ pom-exression ]
  (-> (shell/command ["mvn" "help:evaluate" (str "-Dexpression=" pom-exression) "-q" "-DforceStdout"]
                     {:out :string})
      :out
      string/trim))

(defn local-install []
  (status/line :info "installing project to local maven repo")
  (shell/command ["mvn" "install"]))

(defn get-project []
  (str (get-from-pom "project.groupId") "/" (get-from-pom "project.artifactId") ))

(defn get-version []
  (get-from-pom "project.version"))

;;
;; git
;;

(defn git-sha []
  (-> (shell/command ["git" "rev-parse" "HEAD"]
                     {:out :string})
      :out
      string/trim))

(defn https-uri
  ;; stolen from cljdoc's http-uri
  "Given a URI pointing to a git remote, normalize that URI to an HTTP one."
  [scm-url]
  (cond
    (.startsWith scm-url "http")
    scm-url

    (or (.startsWith scm-url "git@")
        (.startsWith scm-url "ssh://"))
    (-> scm-url
        (string/replace #":" "/")
        (string/replace #"\.git$" "")
        ;; three slashes because of prior :/ replace
        (string/replace #"^(ssh///)*git@" "https://"))))

(defn git-origin-url-as-https []
  (-> (shell/command ["git" "config" "--get" "remote.origin.url"]
                     {:out :string})
      :out
      string/trim
      https-uri))

(defn uncommitted-code? []
  (-> (shell/command ["git" "status" "--porcelain"]
                     {:out :string})
      :out
      string/trim
      seq))

(defn unpushed-commits? []
  (let [{:keys [:exit :out]} (shell/command-no-exit ["git" "cherry" "-v"]
                                                    {:out :string})]
    (if (zero? exit)
      (-> out string/trim seq)
      (status/fatal "Failed to check for unpushed commits to branch, is your branch pushed?"))))

;;
;; docker
;;

(defn status-server [ container ]
  (let [container-id (-> ["docker" "ps" "-q" "-f" (str "name=" (:name container))]
                         (shell/command {:out :string})
                         :out
                         string/trim)]
    (if (string/blank? container-id) "down" "up")))

(defn docker-pull-latest [ container ]
  (shell/command ["docker" "pull" (:image container)]))

(defn stop-server [ container ]
  (when (= "down" (status-server container))
    (status/fatal (str (:name container) " does not appear to be running")))
  (shell/command ["docker" "stop" (:name container) "--time" "0"]))

(defn wait-for-server
  "Wait for container's http server to become available, assumes server has valid root page"
  [ container ]
  (status/line :info (str "Waiting for " (:name container) " to become available"))
  (when (= "down" (status-server container))
    (status/fatal (string/join "\n" [(str  (:name container) " does not seem to be running.")
                                     "Did you run this script with the start command yet?"])))
  (status/line :detail (str (:name container) " container is running"))
  (let [url (str "http://localhost:" (:port container))]
    (loop []
      (try
        (curl/get url)
        (println url "reached")
        (catch Exception _e
          (println "waiting on" url " - hit Ctrl-C to give up")
          (Thread/sleep 4000)
          (recur))))))

(defn status-server-print [container]
  (status/line :detail (str (:name container) ": " (status-server container))))

;;
;; cljdoc server in docker
;;

(defn cljdoc-ingest [container project version]
  (status/line :info (str "Ingesting project " project " " version "\ninto local cljdoc database"))
  (shell/command ["docker"
                  "run" "--rm"
                  "-v" (str cljdoc-db-dir ":/app/data")
                  "-v" (str (home-dir) "/.m2:/root/.m2")
                  "-v" (str (cwd) ":" (cwd) ":ro")
                  "--entrypoint" "clojure"
                  (:image container)
                  "-A:cli"
                  "ingest"
                  ;; project and version are used to locate the maven artifact (presumably locally)
                  "--project" project "--version" version
                  ;; use git origin to support folks working from forks/PRs
                  "--git" (git-origin-url-as-https)
                  ;; specify revision to allow for previewing when working from branch
                  "--rev" (git-sha)]))

(defn start-cljdoc-server [container]
  (when (= "up" (status-server container))
    (status/fatal (str (:name container) " is already running")))
  (status/line :info "Checking for updates")
  (docker-pull-latest container)
  (status/line :info (str "Starting " (:name container) " on port " (:port container)))
  (shell/command ["docker"
                  "run" "--rm"
                  "--name" (:name container)
                  "-d"
                  "-p" (str (:port container) ":8000")
                  "-v" (str cljdoc-db-dir ":/app/data")
                  "-v" (str (home-dir) "/.m2:/root/.m2")
                  "-v" (str (cwd) ":" (cwd) ":ro")
                  (:image container)]))

(defn view-in-browser [url]
  (status/line :info (str "opening " url " in browser"))
  (when (not= 200 (:status (curl/get url {:throw false})))
    (status/fatal (string/join "\n" ["Could not reach:"
                                     url
                                     "\nDid you run this script with ingest command yet?"])))
  (browse/browse-url url))


;;
;; main
;;

(defn git-warnings []
  (let [warnings (remove nil?
                         [(when (uncommitted-code?)
                            "There are changes that have not been committed, they will not be previewed")
                          (when (unpushed-commits?)
                            "There are commits that have not been pushed, they will not be previewed")])]
    (when (seq warnings)
      (status/line :warn (string/join "\n" warnings)))))

(defn cleanup-resources []
  (fs/delete-file-recursively cljdoc-db-dir true))

(defn main [args]

  (check-prerequisites)

  (let [command (first args)]
    (case command
      "start"
      (do
        (start-cljdoc-server cljdoc-container)
        nil)

      "ingest"
      (do
        (git-warnings)
        (local-install)
        (cljdoc-ingest cljdoc-container (get-project) (get-version))
        nil)

      "view"
      (do
        (wait-for-server cljdoc-container)
        (view-in-browser (str "http://localhost:" (:port cljdoc-container) "/d/" (get-project) "/" (get-version)))
        nil)

      "status"
      (status-server-print cljdoc-container)

      "stop"
      (do
        (stop-server cljdoc-container)
        (cleanup-resources)
        nil)

      ;; else
      (do (println "Usage: bb script/cljdoc_preview.clj [start|ingest|view|stop|status]")
          (println "")
          (println " start  - start docker containers supporting cljdoc preview")
          (println " ingest - locally publishes your project for cljdoc preview")
          (println " view   - opens cljdoc preview in your default browser")
          (println " stop   - stops docker containers supporting cljdoc preview")
          (println " status - status of docker containers supporting cljdoc preview")
          (println "")
          (println "Must be run from project root directory.")
          (System/exit 1)))))

(main *command-line-args*)
