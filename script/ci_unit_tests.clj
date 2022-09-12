#!/usr/bin/env bb

(ns ci-unit-tests
  (:require [cheshire.core :as json]
            [doric.core :as doric]
            [helper.fs :as fs]
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
(def ^:private all-oses ["macos" "unbuntu" "windows"])
(def ^:private all-jdks ["8" "11" "17"])

(defn- test-tasks []
  (concat [;; run lintish tasks across all oses to verify that they will work for all devs regardless of their os choice
           {:desc "import-vars" :cmd "bb apply-import-vars check" :oses all-oses :jdks [:default]}
           {:desc "lint"        :cmd "bb lint"                    :oses all-oses :jdks [:default]}
           ;; test-docs on default clojure version across all oses and jdks
           {:desc "test-doc"          :cmd "bb test-doc"          :oses all-oses :jdks all-jdks}]
          (for [version ["1.8" "1.9" "1.10" "1.11"]]
            {:desc (str "clj-" version)
             :cmd (str "bb test-clj --clojure-version " version)
             :oses all-oses
             :jdks all-jdks})
          (for [env [{:param "node" :desc "node"}
                     {:param "chrome-headless" :desc "browser"}]
                opt [{:param "none"}
                     {:param "advanced" :desc "adv"}]]
            {:desc (str "cljs-"
                        (:desc env)
                        (when (:desc opt) (str "-" (:desc opt))))
             :cmd (str "bb test-cljs --env " (:param env) " --optimizations " (:param opt))
             :oses all-oses
             :jdks all-jdks})
          ;; shadow-cljs requires a min of jdk 11
          [{:desc "shadow-cljs"    :cmd "bb test-shadow-cljs" :oses all-oses :jdks ["11" "17"]}]
          ;; planck does not run on windows, and I don't think it uses a jdk
          [{:desc "cljs-bootstrap" :cmd "bb test-cljs --env planck --optimizations none"
            :oses ["macos" "ubuntu"] :jdks [:default]}]))

(defn- test-matrix [default-jdk]
  (for [{:keys [desc cmd oses jdks]} (test-tasks)
        os oses
        jdk jdks
        :let [jdk (if (= :default jdk) default-jdk jdk)]]
    {:desc (str desc " " os " jdk" jdk)
     :cmd cmd
     :os os
     :jdk jdk}))

(defn- clean []
  (doseq [dir ["target" ".cpcache" ".shadow-cljs"]]
    (fs/delete-file-recursively dir true)))

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
      (let [matrix (test-matrix "8")]
        (if (= "json" (get opts "--format"))
          (status/line :detail (json/generate-string matrix))
          (do
            (status/line :detail (doric/table [:os :jdk :desc :cmd] matrix))
            (status/line :detail "Total jobs found: %d" (count matrix)))))
      (let [cur-os (matrix-os)
            cur-jdk (jdk/version)
            cur-major-jdk (str (:major cur-jdk))
            test-list (test-matrix cur-major-jdk)
            tasks-for-cur-env (filter #(and (= cur-os (:os %))
                                            (= cur-major-jdk (:jdk %)))
                                      test-list)
            excluded-for-cur-env (->> (test-tasks)
                                      (remove #(and (some #{cur-os} (:oses %))
                                                    (or (= [:default] (:jdks %))
                                                        (some #{cur-major-jdk} (:jdks %))))))]
        (status/line :detail "Found %d tests suitable for jdk %s on %s"
                     (count tasks-for-cur-env) cur-major-jdk cur-os)
        (doseq [skipped excluded-for-cur-env]
          (status/line :warn "Skipping: %s\nOnly runs on jdks %s and oses %s"
                       (:desc skipped)
                       (:jdks skipped)
                       (:oses skipped)))
        (clean)
        (doseq [{:keys [cmd]} tasks-for-cur-env]
          (shell/command cmd)))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
