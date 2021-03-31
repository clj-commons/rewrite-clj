#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;;

(ns release
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.fs :as fs]
            [helper.shell :as shell]
            [helper.status :as status]
            [release.version :as version]))

(defn clean! []
  (doseq [dir ["target" ".cpcache"]]
    (fs/delete-file-recursively dir true)))

(defn- last-release-tag []
  (->  (shell/command ["git" "describe" "--abbrev=0" "--match" "v[0-9]*"] {:out :string})
       :out
       string/trim))

(defn- calculate-version []
  (status/line :info "Calculating release version")
  (let [version (version/calc)]
    (status/line :detail (str "version: " version))
    version))

(defn- update-file! [fname match-replacements]
  (let [old-content (slurp fname)
        new-content (reduce (fn [in [desc match replacement]]
                              (let [out (string/replace-first in match replacement)]
                                (if (= in out)
                                  (status/fatal (format "Expected to %s in %s" desc fname))
                                  out)))
                            old-content
                            match-replacements)]
    (spit fname new-content)))

(defn- update-user-guide! [version]
  (status/line :info (str "Updating project version in user guide to " version))
  (update-file! "doc/01-user-guide.adoc"
                [["update version in deps.edn example"
                  #"(?m)(^ *rewrite-clj/rewrite-clj *\{ *:mvn/version *\").*(\" *\} *$)"
                  (str "$1" version "$2")]
                 ["update version in leiningen example"
                  #"(?m)(^ *\[ *rewrite-clj/rewrite-clj *\").*(\" *\] *$)"
                  (str "$1" version "$2")]]))

(defn- adoc-section-search[content find-section]
  (cond
    (re-find (re-pattern  (str  "(?ims)^=+ " find-section " *$\\s+(^=|\\z)")) content)
    :found-with-no-text

    (re-find (re-pattern  (str  "(?ims)^=+ " find-section " *$")) content)
    :found

    :else
    :not-found))

(defn- validate-changelog
  "Certainly not fool proof, but should help for common mistakes"
  []
  (status/line :info "Validating change log")
  (let [content (slurp "CHANGELOG.adoc")
        unreleased-status (adoc-section-search content "Unreleased")
        unreleased-breaking-status (adoc-section-search content "Unreleased Breaking Changes")]
    (case unreleased-status
      :found (status/line :detail "✅ Unreleased section found with descriptive text.")
      :found-with-no-text (status/line :detail "❌ Unreleased section seems empty, please put some descriptive text in there to help our users understand what is in the release.")
      :not-found (status/line :detail "❌ Unreleased section not found, please add one."))
    (case unreleased-breaking-status
      :found (status/line :detail "✅ Unreleased Breaking Changes section found with descriptive text")
      :found-with-no-text (status/line :detail (str "❌ Unreleased breaking changes section found but seems empty.\n"
                                                    "   Please put some descriptive text in there to help our users understand what is in the release\n"
                                                    "   OR delete the section if there are no breaking changes for this release."))
      :not-found (status/line :detail "✅ Unreleased Breaking Changes section not found, assuming no breaking changes for this release."))

    (if (or (not (= :found unreleased-status))
            (= :found-with-no-text unreleased-breaking-status))
      (status/fatal "Changelog needs some love")
      (status/line :detail "Changelog looks good for update by release workflow."))
    {:unreleased unreleased-status
     :unreleased-breaking unreleased-breaking-status}))

(defn- update-changelog! [version last-version {:keys [unreleased-breaking]}]
  (status/line :info (str "Updating Change Log unreleased headers to release " version))
  (update-file! "CHANGELOG.adoc"
                (cond-> [["update unreleased header"
                          #"(?ims)^(=+) +unreleased *$(.*?)(^=+)"
                          (str "$1 Unreleased\n\n$1 v" version "$2"
                               (when last-version
                                 (str
                                  "https://github.com/clj-commons/rewrite-clj/compare/"
                                  last-version
                                  "\\\\...v"  ;; single backslash is escape for AsciiDoc
                                  version
                                  "[Gritty details of changes for this release]\n\n"))
                               "$3")]]
                  (= :found unreleased-breaking)
                  (conj ["update unreleased breaking change header"
                         #"(?im)(=+) +unreleased breaking changes *$"
                         (str "$1 v" version)]))))

(defn- create-jar! [version]
  (status/line :info (str "Creating jar for version " version))
  (status/line :detail "Reflecting deps in deps.edn to pom.xml")
  (shell/command ["clojure" "-Spom"])
  (status/line :detail "Updating pom.xml version and creating thin jar")
  (shell/command ["clojure" "-X:jar" ":version" (pr-str version)])
  nil)

(defn- assert-on-ci
  "Little blocker to save myself from myself when testing."
  [action]
  (when (not (System/getenv "CI"))
    (status/fatal (format  "We only want to %s from CI" action))))

(defn- deploy-jar!
  "For this to work, appropriate CLOJARS_USERNAME and CLOJARS_PASSWORD must be in environment."
  []
  (status/line :info "Deploying jar to clojars")
  (assert-on-ci "deploy a jar")
  (shell/command ["clojure" "-X:deploy:remote"])
  nil)

(defn- commit-changes! [version]
  (let [tag-version (str "v" version)]
    (status/line :info (str  "Committing and pushing changes made for " tag-version))
    (assert-on-ci "commit changes")
    (status/line :detail "Adding changes")
    (shell/command ["git" "add" "doc/01-user-guide.adoc" "CHANGELOG.adoc" "pom.xml"])
    (status/line :detail "Committing")
    (shell/command ["git" "commit" "-m" (str  "Release job: updates for version " tag-version)])
    (status/line :detail "Version tagging")
    (shell/command ["git" "tag" "-a" tag-version "-m" (str  "Release " tag-version)])
    (status/line :detail "Pushing commit")
    (shell/command ["git" "push"])
    (status/line :detail "Pushing version tag")
    (shell/command ["git" "push" "origin" tag-version])
    nil))

(defn- inform-cljdoc! [version]
  (status/line :info (str "Informing cljdoc of new version " version))
  (assert-on-ci "inform cljdoc")
  (let [exit-code (->  (shell/command-no-exit ["curl" "-X" "POST"
                                               "-d" "project=rewrite-clj/rewrite-clj"
                                               "-d" (str  "version=" version)
                                               "https://cljdoc.org/api/request-build2"])
                       :exit)]
    (when (not (zero? exit-code))
      (status/line :warn (str  "Informing cljdoc did not seem to work, exited with " exit-code)))))


(defn- validate-args [args]
  (let [cmd (first args)]
    (when (or (not= 1 (count args))
              (not (some #{cmd} '("prep" "deploy-remote" "commit" "validate" "version"))))
      (status/fatal (string/join "\n"
                                 ["Usage: release cmd"
                                  ""
                                  "Where cmd can be:"
                                  " prep           - update user guide, changelog and create jar"
                                  " deploy-remote  - deploy jar to clojars"
                                  " commit         - commit changes made back to repo, inform cljdoc of release"
                                  ""
                                  "These commands are expected to be run in order from CI."
                                  "Why the awkward separation?"
                                  "To restrict the exposure of our CLOJARS secrets during deploy workflow"
                                  ""
                                  "Additional commands:"
                                  " validate      - verify that change log is good for release"
                                  " version       - calculate and report version"])))
    cmd))

(defn- main [args]
  (let [cmd (validate-args args)
        target-version-filename "target/target-version.txt"]
    (status/line :info (str  "Attempting release step: " cmd))
    (case cmd
      "prep"
      (do (clean!)
          (let [changelog-status (validate-changelog)
                target-version (calculate-version)
                last-version (last-release-tag)]
            (status/line :detail (str "Last version released: " (or last-version "<none>")))
            (status/line :detail (str "Target version:        " target-version))
            (io/make-parents target-version-filename)
            (spit target-version-filename target-version)
            (update-user-guide! target-version)
            (update-changelog! target-version last-version changelog-status)
            (create-jar! target-version)))

      "deploy-remote"
      (deploy-jar!)

      "commit"
      (if (not (.exists (io/file target-version-filename)))
        (status/fatal (str "Target version file not found: " target-version-filename
                           "\nWas prep step run?"))
        (let [target-version (slurp target-version-filename)]
          (commit-changes! target-version)
          (inform-cljdoc! target-version)))

      "validate"
      (do (validate-changelog)
          nil)

      "version"
      (do (calculate-version)
          nil))

    (status/line :detail (str "Release step done:" cmd))))

(main *command-line-args*)
