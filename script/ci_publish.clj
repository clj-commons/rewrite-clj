(ns ci-publish
  "Publish work we invoke from GitHub Actions.
  Separated out here:
  - to make it clear what is happening on ci
  - rate of change here should be less/different than in publish namespace"
  (:require [babashka.tasks :as t]
            [build-shared]
            [lread.status-line :as status]))

(def ^:private changelog-url (format "https://github.com/%s/blob/master/CHANGELOG.adoc" (build-shared/lib-github-coords)))

(defn- assert-on-ci []
  (when (not (System/getenv "CI"))
    (status/die 1 "to be run from continuous integration server only")))

(defn- ci-tag []
  (when (= "tag" (System/getenv "GITHUB_REF_TYPE"))
    (System/getenv "GITHUB_REF_NAME")))

(defn- analyze-ci-tag []
  (let [tag (ci-tag)]
    (if (not tag)
      (status/die 1 "CI tag not found")
      (let [version-from-tag (build-shared/tag->version tag)
            lib-version (build-shared/lib-version)]
        (cond
          (not version-from-tag)
          (status/die 1 "Not recognized as version tag: %s" tag)

          (not= version-from-tag lib-version)
          (status/die 1 "Lib version %s does not match version from tag %s"
                        lib-version version-from-tag)
          :else
          {:tag tag
           :version lib-version})))))

;;
;; Task entry points
;;

(defn clojars-deploy []
  (assert-on-ci)
  (analyze-ci-tag) ;; fail on unexpected version tag
  (t/shell "clojure -T:build deploy"))

(defn github-create-release []
  (assert-on-ci)
  (let [{:keys [tag]} (analyze-ci-tag)]
    (t/shell "gh release create"
             tag
             "--title" tag
             "--notes" (format "[Changelog](%s#%s)" changelog-url tag))))

(defn cljdoc-request-build []
  (assert-on-ci)
  (let [{:keys [version]} (analyze-ci-tag)
        lib (build-shared/lib-artifact-name)]
    (status/line :head "Informing cljdoc of %s version %s" lib version)
    (assert-on-ci)
    (let [exit-code (->  (t/shell {:continue true}
                                  "curl" "-X" "POST"
                                  "-d" (str "project=" lib)
                                  "-d" (str "version=" version)
                                  "https://cljdoc.org/api/request-build2")
                         :exit)]
      (when (not (zero? exit-code))
        (status/line :warn (str  "Informing cljdoc did not seem to work, exited with " exit-code))))))
