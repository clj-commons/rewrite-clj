(defproject rewrite-clj "0.1.0-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/xsc/rewrite-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.7.5"]
                 [potemkin "0.3.1"]
                 [fast-zip "0.3.0"]]
  :profiles {:test {:dependencies [[midje "1.5.1"]]
                    :plugins [[lein-midje "3.0.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :aliases {"midje"     ["with-profile" "test" "midje"]
            "midje-all" ["with-profile" "test,1.4:test,1.5:test,1.6" "midje"]
            "deps-all" ["with-profile" "test,1.4:test,1.5:test,1.6" "deps"]})
