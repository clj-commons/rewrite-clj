(defproject rewrite-clj "0.3.5"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/xsc/rewrite-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.8.2"]
                 [fast-zip "0.3.0"]
                 [potemkin "0.3.4"]]
  :profiles {:dev {:dependencies [[midje "1.6.0" :exclusions [joda-time]]
                                  [joda-time "2.3"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :aliases {"midje-all" ["with-profile" "dev,1.4:dev,1.5:dev,1.6" "midje"]
            "deps-all" ["with-profile" "dev,1.4:dev,1.5:dev,1.6" "deps"]}
  :pedantic? :abort)
