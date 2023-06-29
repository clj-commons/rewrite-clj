#!/usr/bin/env bb

(ns doc-api-diffs
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn install-locally []
  (status/line :head "installing rewrite-clj v1 locally from dev")
  (shell/command "mvn install"))

(defn wipe-rewrite-clj-diff-cache [ {:keys [coords version]}]
  (let [cache-dir (io/file "./.diff-apis/.cache")
        proj-cache-prefix (str  (string/replace coords "/" "-") "-" version)]
    (when-let [proj-cache-dir (and (.exists cache-dir)
                                   (.isDirectory cache-dir)
                                   (first (filter
                                           #(string/starts-with? (str (.getName %)) proj-cache-prefix)
                                           (.listFiles cache-dir))))]
      (println "- removing " proj-cache-dir)
      (fs/delete-tree proj-cache-dir))))

(defn clean [{:keys [:report-dir]} rewrite-clj-v1-coords]
  (status/line :head "Clean")
  (status/line :detail "- report dir")
  (fs/delete-tree report-dir)
  (.mkdirs (io/file report-dir))
  (status/line :detail "- cached metdata for rewrite-clj v1 (because that's the thing that is changing, right?)")
  (wipe-rewrite-clj-diff-cache rewrite-clj-v1-coords)
  (status/line :detail "all done"))

(defn describe-proj [project]
  (str (or (:as-coords project)
           (:coords project)) " " (:version project) " " (:lang project)))

(defn diff-apis [{:keys [:notes-dir :report-dir]} projecta projectb report-name extra-args]
  (status/line :head "Diffing %s and %s" (describe-proj projecta) (describe-proj projectb))
  (apply shell/command
         (concat
          ["clojure" "-M:diff-apis"]
          (map projecta [:coords :version :lang])
          (map projectb [:coords :version :lang])
          ["--arglists-by" ":arity-only"
           "--notes" (str (io/file notes-dir (str report-name ".adoc")))
           "--report-format" ":asciidoc"
           "--report-filename" (str  (io/file report-dir (str report-name ".adoc")))]
          extra-args)))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (let [opts {:notes-dir "doc/diff-notes"
                :report-dir "doc/generated/api-diffs"}
          rewrite-clj-v0-lang-clj  {:coords "rewrite-clj/rewrite-clj" :version "0.6.1" :lang "clj"}
          rewrite-cljs-lang-cljs   {:coords "rewrite-cljs/rewrite-cljs" :version "0.4.5" :lang "cljs"}
          rewrite-clj-v1-lang-clj {:coords "rewrite-clj/rewrite-clj" :version "1.0.0-alpha" :lang "clj"}
          rewrite-clj-v1-lang-cljs (assoc rewrite-clj-v1-lang-clj :lang "cljs")
          existing-to-cljc-args ["--exclude-namespace" "rewrite-clj"
                                 "--exclude-namespace" "rewrite-clj.potemkin"
                                 "--exclude-namespace" "rewrite-clj.custom-zipper.switchable"
                                 "--exclude-namespace" "rewrite-clj.interop"]
          to-self-args ["--exclude-namespace" "rewrite-clj.potemkin.clojure"]
          documented-only-args ["--exclude-with" ":no-doc" "--exclude-with" ":skip-wiki"]]
      (install-locally)
      (clean opts rewrite-clj-v1-lang-clj)
      (diff-apis opts rewrite-clj-v0-lang-clj    rewrite-cljs-lang-cljs    "rewrite-clj-v0-lang-clj-and-rewrite-cljs-lang-cljs"                   [])
      (diff-apis opts rewrite-clj-v0-lang-clj    rewrite-clj-v1-lang-clj   "rewrite-clj-v0-lang-clj-and-rewrite-clj-v1-lang-clj"                  existing-to-cljc-args)
      (diff-apis opts rewrite-cljs-lang-cljs     rewrite-clj-v1-lang-cljs  "rewrite-cljs-lang-cljs-and-rewrite-clj-v1-lang-cljs"                  existing-to-cljc-args)
      (diff-apis opts rewrite-clj-v1-lang-cljs   rewrite-clj-v1-lang-clj   "rewrite-clj-v1-lang-cljs-and-rewrite-clj-v1-lang-clj"                 to-self-args)
      (diff-apis opts rewrite-clj-v1-lang-cljs   rewrite-clj-v1-lang-clj   "rewrite-clj-v1-lang-cljs-and-rewrite-clj-v1-lang-clj-documented-only" (concat to-self-args documented-only-args))))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
