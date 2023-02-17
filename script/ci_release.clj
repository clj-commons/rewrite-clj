#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;;


(ns ci-release
  (:require [build-shared :as bu]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.fs :as fs]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn clean! []
  (doseq [dir ["target" ".cpcache"]]
    (fs/delete-file-recursively dir true)))

(defn- last-release-tag []
  (->  (shell/command {:out :string}
                      "git describe --abbrev=0 --match v[0-9]*")
       :out
       string/trim))

(defn- update-file! [fname match-replacements]
  (let [old-content (slurp fname)
        new-content (reduce (fn [in [desc match replacement]]
                              (let [out (string/replace-first in match replacement)]
                                (if (= in out)
                                  (status/die 1 "Expected to %s in %s" desc fname)
                                  out)))
                            old-content
                            match-replacements)]
    (spit fname new-content)))

(defn- update-user-guide! [version]
  (status/line :head (str "Updating project version in user guide to " version))
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
  (status/line :head "Validating change log")
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
      (status/die 1 "Changelog needs some love")
      (status/line :detail "Changelog looks good for update by release workflow."))
    {:unreleased unreleased-status
     :unreleased-breaking unreleased-breaking-status}))

(defn- update-changelog! [version last-version {:keys [unreleased-breaking]}]
  (status/line :head (str "Updating Change Log unreleased headers to release " version))
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

(defn- create-jar! []
  (status/line :head "Creating jar for release")
  (shell/command "clojure -T:build jar")
  nil)

(defn- built-version []
  (-> (shell/command {:out :string}
                     "clojure -T:build built-version")
      :out
      string/trim))

(defn- assert-on-ci
  "Little blocker to save myself from myself when testing."
  [action]
  (when (not (System/getenv "CI"))
    (status/die 1 "We only want to %s from CI" action)))

(defn- deploy-jar!
  "For this to work, appropriate CLOJARS_USERNAME and CLOJARS_PASSWORD must be in environment."
  []
  (status/line :head "Deploying jar to clojars")
  (assert-on-ci "deploy a jar")
  (shell/command "clojure -T:build deploy")
  nil)

(defn- commit-changes! [version]
  (let [tag-version (str "v" version)]
    (status/line :head (str  "Committing and pushing changes made for " tag-version))
    (assert-on-ci "commit changes")
    (status/line :detail "Adding changes")
    (shell/command "git add doc/01-user-guide.adoc CHANGELOG.adoc version.edn")
    (status/line :detail "Committing")
    (shell/command "git commit -m" (str  "Release job: updates for version " tag-version))
    (status/line :detail "Version tagging")
    (shell/command "git" "tag" "-a" tag-version "-m" (str  "Release " tag-version))
    (status/line :detail "Pushing commit")
    (shell/command "git push")
    (status/line :detail "Pushing version tag")
    (shell/command "git push origin" tag-version)
    nil))

(defn create-github-release [version]
  (let [tag-version (str "v" version)
        changelog-url (format "https://github.com/clj-commons/rewrite-clj/blob/master/CHANGELOG.adoc")]
    (status/line :head (str "Creating GitHub release for tag" tag-version))
    (shell/command "gh release create"
                   tag-version
                   "--title" tag-version
                   "--notes" (format "[Changelog](%s#%s)" changelog-url tag-version))))

(defn- inform-cljdoc! [version]
  (status/line :head (str "Informing cljdoc of new version " version))
  (assert-on-ci "inform cljdoc")
  (let [exit-code (->  (shell/command {:continue true}
                                      "curl" "-X" "POST"
                                      "-d" "project=rewrite-clj/rewrite-clj"
                                      "-d" (str  "version=" version)
                                      "https://cljdoc.org/api/request-build2")
                       :exit)]
    (when (not (zero? exit-code))
      (status/line :warn (str  "Informing cljdoc did not seem to work, exited with " exit-code)))))

(def args-usage "Valid args: (prep|deploy-remote|commit|validate|--help)

Commands:
  prep           Update user guide, changelog and create jar
  deploy-remote  Deploy jar to clojars
  commit         Commit changes made back to repo, inform cljdoc of release

These commands are expected to be run in order from CI.
Why the awkward separation?
To restrict the exposure of our CLOJARS secrets during deploy workflow

Additional commands:
  validate      Verify that change log is good for release

Options
  --help        Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "prep")
      (do (clean!)
          (let [changelog-status (validate-changelog)
                last-version (last-release-tag)]
            (status/line :detail (str "Last version released: " (or last-version "<none>")))
            (io/make-parents "target")
            (bu/bump-version)
            (create-jar!)
            (let [version (built-version)]
              (status/line :detail (str "Built version: " version))
              (update-user-guide! version)
              (update-changelog! version last-version changelog-status))))

      (get opts "deploy-remote")
      (deploy-jar!)

      (get opts "commit")
      (let [version (built-version)]
        (commit-changes! version)
        (create-github-release version)
        (inform-cljdoc! version))

      (get opts "validate")
      (do (validate-changelog)
          nil))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
