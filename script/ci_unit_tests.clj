#!/usr/bin/env bb

(ns ci-unit-tests
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.jdk :as jdk]
            [helper.main :as main]
            [helper.os :as os]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- matrix-os []
  (case (os/get-os)
    :win "windows"
    :mac "macos"
    ;; else assume ubuntu
    "ubuntu"))

;; matrix params to be used on ci
(def ^:private os-jdks {"ubuntu" ["8" "11" "17" "21" "23"]
                        ;; macOS on GitHub Actions is now arm-based and does not include jdk8
                        "macos"   ["11" "17" "21" "23"]
                        "windows" ["8" "11" "17" "21" "23"]})
(def ^:private all-oses (keys os-jdks))

(defn- test-tasks []
  (concat [;; run lintish tasks across all oses to verify that they will work for all devs regardless of their os choice
           {:desc "import-vars" :cmd "bb apply-import-vars check" :oses all-oses :jdks :earliest}
           {:desc "lint"        :cmd "bb lint"                    :oses all-oses :jdks :earliest}
           ;; test-docs on default clojure version across all oses and jdks
           {:desc "test-doc"          :cmd "bb test-doc"          :oses all-oses :jdks :all :requires ["npm"]}]
          (for [version ["all"]]
            {:desc (str "clj-" version)
             :cmd (str "bb test-clj --clojure-version " version)
             :oses all-oses
             :jdks :all})
          ;; I'm not sure there's much value testing across jdks for ClojureScript tests, for now we'll stick with earliest jdk only
          (for [env [{:param "node" :desc "node"}
                     {:param "chrome-headless" :desc "browser"}]
                opt [{:param "none"}
                     {:param "advanced" :desc "adv"}]]
            {:desc (str "cljs-"
                        (:desc env)
                        (when (:desc opt) (str "-" (:desc opt))))
             :cmd (str "bb test-cljs --env " (:param env) " --optimizations " (:param opt))
             :oses all-oses
             :jdks :earliest
             :requires ["npm"]})
          ;; shadow-cljs requires a min of jdk 11 so we'll test on that
          [{:desc "shadow-cljs"    :cmd "bb test-shadow-cljs" :oses all-oses :jdks ["11"]
            :skip-reason-fn (fn [{:keys [jdk]}] (when (< (parse-long jdk) 11)
                                                  "jdk must be >= 11"))
            :requires ["npm"]}]
          ;; planck does not run on windows, and I don't think it needs a jdk
          [{:desc "cljs-bootstrap" :cmd "bb test-cljs --env planck --optimizations none"
            :oses ["macos" "ubuntu"] :jdks :earliest :requires ["planck"]}]))

(defn- ci-test-matrix []
  (for [{:keys [desc cmd oses jdks requires]} (test-tasks)
        os oses
        jdk (case jdks
              :earliest (take 1 (get os-jdks os))
              :all (get os-jdks os)
              jdks)]
    {:desc (str desc " " os " jdk" jdk)
     :cmd cmd
     :os os
     :jdk jdk
     :requires (or requires [])}))

(defn- local-test-list [local-os local-jdk]
  (for [{:keys [desc cmd oses skip-reason-fn]} (test-tasks)]
   (let [skip-reasons (cond-> []
                        (not (some #{local-os} oses))
                        (conj (str "os must be among " oses))
                        (and skip-reason-fn (skip-reason-fn {:jdk local-jdk}))
                        (conj (skip-reason-fn {:jdk local-jdk})))]
     (cond-> {:desc desc
              :cmd cmd}
       (seq skip-reasons)
       (assoc :skip-reasons skip-reasons)))))

(defn- clean []
  (doseq [dir ["target" ".cpcache" ".shadow-cljs"]]
    (fs/delete-tree dir)))

(def args-usage "Valid args:
  [matrix-for-ci [--format=json]]
  --help

Commands:
  matrix-for-ci Return a matrix for use within GitHub Actions workflow

Options:
  --help    Show this help

By default, will run all tests applicable to your current jdk and os.")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (if (get opts "matrix-for-ci")
      (let [matrix (ci-test-matrix)]
        (if (= "json" (get opts "--format"))
          (status/line :detail (json/generate-string matrix))
          (do
            (status/line :detail (doric/table [:os :jdk :desc :cmd :requires] matrix))
            (status/line :detail "Total jobs found: %d" (count matrix)))))
      (let [cur-os (matrix-os)
            cur-jdk (jdk/version)
            cur-major-jdk (str (:major cur-jdk))
            ci-jdks (get os-jdks cur-os)
            test-list (local-test-list cur-os cur-major-jdk)
            tests-skipped (filter :skip-reasons test-list)
            tests-to-run (remove :skip-reasons test-list)]
        (when (not (some #{cur-major-jdk} ci-jdks))
          (status/line :warn "CI runs only on jdks %s on %s OS but you are on jdk %s\nWe'll run tests anyway."
                       ci-jdks cur-os (:version cur-jdk)))

        (status/line :head "Test plan")
        (status/line :detail "Found %d runnable tests for jdk %s on %s:"
                     (count tests-to-run) cur-major-jdk cur-os)
        (doseq [{:keys [cmd]} tests-to-run]
          (status/line :detail (str " " cmd)))
        (doseq [{:keys [cmd skip-reasons]} tests-skipped]
          (status/line :warn (string/join "\n* "
                                          (concat [(str "Skipping: " cmd)]
                                                  skip-reasons))))
        (clean)
        (doseq [{:keys [cmd]} tests-to-run]
          (shell/command cmd)))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
