#!/usr/bin/env bb

(ns doc-update-readme
  "Script to update README.adoc to credit contributors
  Run manually as needed."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [helper.env :as env]
            [helper.fs :as fs]
            [helper.main :as main]
            [helper.shell :as shell]
            [hiccup.util :as hu]
            [hiccup2.core :as h]
            [lread.status-line :as status])
  (:import (java.nio.file Files Paths CopyOption StandardCopyOption)
           (java.nio.file.attribute FileAttribute)))

(def contributions-lookup
  {:code-rewrite-clj-v1  "💻 rewrite-clj v1"
   :code-rewrite-cljs    "💻 rewrite-cljs"
   :code-rewrite-clj-v0  "💻 rewrite-clj v0"
   :encouragement        "🌞 encouragement"
   :education            "🎓 enlightenment"
   :original-author      "👑 original author"
   :infrastructure       "⚙️ infrastructure"})

(defn- generate-asciidoc [contributors {:keys [images-dir image-width]}]
  (str ":imagesdir: " images-dir "\n"
       "[.float-group]\n"
       "--\n"
       (apply str (for [{:keys [github-id]} contributors]
                    (str "image:" github-id ".png[" github-id ",role=\"left\",width=" image-width ",link=\"https://github.com/" github-id "\"]\n")))
       "--\n"))

(defn- update-readme-text [old-text marker-id new-content]
  (let [marker (str "// AUTO-GENERATED:" marker-id )
        marker-start (str marker "-START")
        marker-end (str marker "-END")]
    (string/replace old-text
                    (re-pattern (str "(?s)" marker-start ".*" marker-end))
                    (str marker-start "\n" (string/trim new-content) "\n" marker-end))))

(defn update-readme-file! [contributors readme-filename image-info]
  (status/line :head (str "updating " readme-filename))
  (let [old-text (slurp readme-filename)
        new-text (-> old-text
                     (update-readme-text "CONTRIBUTORS" (generate-asciidoc (:contributors contributors) image-info))
                     (update-readme-text "FOUNDERS" (generate-asciidoc (:founders contributors) image-info))
                     (update-readme-text "MAINTAINERS" (generate-asciidoc (:maintainers contributors) image-info)))]
    (if (not (= old-text new-text))
      (do
        (spit readme-filename new-text)
        (status/line :detail (str  readme-filename " text updated")))
      (status/line :detail (str  readme-filename " text unchanged")))))

(defn include-css
  ;; not in babashka, adpapted from hiccup.page
  "Include a list of external stylesheet files."
  [& styles]
  (for [style styles]
    [:link {:type "text/css", :href style, :rel "stylesheet"}]))

(defn generate-contributor-html [github-id contributions]
  (str
   (h/html
    [:head
     (include-css "https://fonts.googleapis.com/css?family=Fira+Code&display=swap")
     [:style
      (hu/raw-string
       "* {
          -webkit-font-smoothing: antialiased;
          -moz-osx-font-smoothing: grayscale;}
        body {
          font-family: 'Fira Code', monospace;
          margin: 0;}
        .card {
          min-width: 295px;
          float: left;
          border-radius: 5px;
          border: 1px solid #CCCCCC;
          padding: 4px;
          margin: 0 5px 5px 0;
          box-shadow: 4px 4px 3px grey;
          background-color: #F4F4F4;}
        .avatar {
          float: left;
          height: 110px;
          border-radius: 4px;
          padding: 0;
          margin-right: 6px; }
        .image { margin: 2px;}
        .text {
          margin-left: 2px;
          padding: 0}
        .contrib { margin: 0; }
        .name {
          font-size: 1.20em;
          margin: 0 3px 5px 0;}")]]
    [:div.card
     [:img.avatar {:src (str "https://github.com/" github-id ".png?size=110")}]
     [:div.text
      [:p.name (str "@" github-id)]
      [:div.contribs
       (doall
        (for [c contributions]
          (when-let [c-text (c contributions-lookup)]
            [:p.contrib c-text])))]]])))

(defn- str->Path [spath]
  (Paths/get spath (into-array String [])))

  (defn- temp-Path [prefix]
  (Files/createTempDirectory prefix (into-array FileAttribute [])))

(defn- move-Path [source target]
  (fs/delete-file-recursively (.toFile target))
  (.mkdirs (.toFile target))
  (Files/move source target (into-array CopyOption
                                        [(StandardCopyOption/ATOMIC_MOVE)
                                         (StandardCopyOption/REPLACE_EXISTING)])))

(defn- chrome []
  (let [mac-chrome "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        linux-chrome "chrome"]
    (cond
      (.canExecute (io/file mac-chrome)) mac-chrome
      :else linux-chrome)))

(defn- chrome-info []
  (try
    (let [chrome (chrome)]
      {:exe chrome
       :version (-> (shell/command [chrome "--version"] {:out :string})
                    :out
                    string/trim)})
    (catch Exception _e)))

(defn- generate-image! [target-dir github-id contributions image-opts]
  (let [html-file (str target-dir "/temp.html")]
    (try
      (spit html-file (generate-contributor-html github-id contributions))
      (shell/command [(chrome)
                      "--headless"
                      (str "--screenshot=" target-dir "/" github-id ".png")
                      (str "--window-size=" (:image-width image-opts) ",125")
                      "--default-background-color=0"
                      "--hide-scrollbars"
                      html-file]
                     {:out :string :err :string})
      (finally
        (fs/delete-file-recursively (io/file html-file) true)))))

(defn- generate-contributor-images! [contributors image-opts]
  (status/line :head "generating contributor images")
  (let [work-dir (temp-Path "rewrite-clj-update-readme")]
    (try
      (doall
       (for [contributor-type (keys contributors)]
         (do
           (status/line :detail (str contributor-type))
           (doall
            (for [{:keys [github-id contributions]} (contributor-type contributors)]
              (do
                (status/line :detail (str "  " github-id " " contributions))
                (generate-image! (str work-dir) github-id contributions image-opts)))))))
      (let [target-path (str->Path (:images-dir image-opts))]
        (move-Path work-dir target-path))
      (catch java.lang.Exception e
        (fs/delete-file-recursively (.toFile work-dir) true)
        (throw e)))))

(defn- sort-contributors
  "Maybe not perfect but the aim for now is to sort by number of contributions then github id.
   Maybe I should just sort by github id?"
  [contributors]
  (reduce-kv (fn [m k v]
               (assoc m k (sort-by (juxt #(- (count (:contributions %)))
                                         #(string/lower-case (:github-id %)))
                                   v)))
             {}
             contributors))

(defn- check-prerequesites []
  (status/line :head  "checking prerequesites")
  (let [chrome-info (chrome-info)]
    (if chrome-info
      (status/line :detail (str "found chrome:" (:exe chrome-info) "\n"
                                "version:" (:version chrome-info)))
      (status/line :detail "* error: did not find google chrome - need it to generate images."))
    chrome-info))

(defn -main [& args]
  (main/run-argless-cmd
   args
   (fn []
     (let [readme-filename "README.adoc"
           contributors-source "doc/contributors.edn"
           image-opts {:image-width 310
                       :images-dir "./doc/generated/contributors"}
           contributors (->> (slurp contributors-source)
                             edn/read-string
                             sort-contributors)]
       (status/line :head "updating docs to honor those who contributed")
       (when (not (check-prerequesites))
         (status/die 1 "pre-requisites not met"))
       (status/line :detail (str  "contributors source: " contributors-source))
       (generate-contributor-images! contributors image-opts)
       (update-readme-file! contributors readme-filename image-opts)
       (status/line :detail "SUCCESS"))))
  (shutdown-agents))

(env/when-invoked-as-script
 (apply -main *command-line-args*))

