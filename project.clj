(defproject rewrite-clj "0.6.1-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/xsc/rewrite-clj"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/tools.reader "0.10.0"]]
  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [joda-time]]
                                  [joda-time "2.9.3"]
                                  [org.clojure/test.check "0.7.0"]]
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
