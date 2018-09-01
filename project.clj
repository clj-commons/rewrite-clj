(defproject rewrite-clj "0.6.1"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/xsc/rewrite-clj"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/tools.reader "1.2.2"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.7.0"]]
                   :plugins [[codox "0.8.10"]]
                   :exclusions [org.clojure/clojure]
                   :codox {:project {:name "rewrite-clj"}
                           :defaults {:doc/format :markdown}}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :aliases {"all" ["with-profile" "dev,1.5:dev,1.6:dev,1.7:dev,1.8:dev"]}
  :pedantic? :abort)
