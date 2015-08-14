(defproject rewrite-clj "0.4.13-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/xsc/rewrite-clj"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.9.2"]]
  :profiles {:dev {:dependencies [[midje "1.7.0" :exclusions [joda-time]]
                                  [joda-time "2.8.2"]]
                   :plugins [[lein-midje "3.1.3"]
                             [codox "0.8.10"]]
                   :exclusions [org.clojure/clojure]
                   :codox {:project {:name "rewrite-clj"}
                           :defaults {:doc/format :markdown}}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :aliases {"all" ["with-profile" "dev,1.5:dev,1.6:dev,1.7"]
            "test" ["midje"]
            "test-ancient" ["with-profile" "dev,1.5:dev,1.6:dev,1.7" "midje"]}
  :pedantic? :abort)
