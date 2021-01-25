(ns clj-graal.gen-test-runner
  (:require [cli-matic.core :as cli-matic]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.namespace.find :as find]))

;;
;; to run generated test runner:
;; java -cp "target/clj-graal/generated:$(clojure -M:test-common -Spath)" clojure.main -m clj-graal.test-runner
;;

(defn- in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn- nses[]
  (->> (find/find-namespaces [(io/file "test")] find/clj)
       (filter #(re-matches #".*-test" (str %)))))

(defn- vars[test-nses]
  (->> test-nses
       (mapcat (fn [ns]
                 (require ns)
                 (->> (the-ns ns)
                      ns-publics
                      vals
                      (filter #(:test (meta %))))))))


(defn- prep-runner-src-file [dest-dir]
  (let [dir (io/file dest-dir "clj_graal")]
    (.mkdirs dir)
    (io/file dir "test_runner.clj")))

(defn- vars-to-test [include-vars]
  (let [all-nses (nses)
        all-vars (vars all-nses)
        vars (if (seq include-vars)
               (filter #(in? include-vars (str (symbol %))) all-vars)
               all-vars)
        nses (distinct (map #(namespace (symbol %)) vars))]
    {:vars vars :nses nses}))

(defn- namespaces-to-test [include-nses]
  (let [all-nses (nses)
        nses (if (seq include-nses)
               (filter #(in? include-nses (str (symbol %))) all-nses)
               all-nses)]
    nses))

(defn- interpret-vars-args [{:keys [include dest-dir] :as opts}]
  (println "opts" opts)
  (let [{:keys [vars nses]} (vars-to-test include)
        runner-src-file (prep-runner-src-file dest-dir)]
    (println "- target:" (str runner-src-file))
    (println "- namespaces:" (count nses))
    (println "- vars:" (count vars))
    (when (zero? (count vars))
      (println "* error: no test vars found")
      (System/exit 1))
    {:vars vars :nses nses :runner-src-file runner-src-file}))

(defn- interpret-namespace-args [{:keys [include dest-dir] :as opts}]
  (println "opts" opts)
  (let [nses (namespaces-to-test include)
        runner-src-file (prep-runner-src-file dest-dir)]
    (println "- target:" (str runner-src-file))
    (println "- namespaces:" (count nses))
    (when (zero? (count nses))
      (println "* error: no test namespaces found")
      (System/exit 1))
    {:nses nses :runner-src-file runner-src-file}))

(defn- cmd-find-all-vars[_opts]
  (->> (nses)
       vars
       (map #(symbol %))
       sort
       (string/join " ")
       println))

(defn- cmd-find-all-namespaces[_opts]
  (->> (nses)
       sort
       (string/join " ")
       println))

(defn- cmd-generate-test-by-namespace[opts]
  (let [{:keys [nses runner-src-file]} (interpret-namespace-args opts)]
    (spit runner-src-file
          (-> (slurp (io/resource "clj_graal/template/by_ns_test_runner.clj.template"))
              (.replace "#_@TEST_NSES_HERE" (string/join "\n" nses))))
    (println "done")))

(defn- cmd-generate-test-by-var[opts]
  (let [{:keys [vars nses runner-src-file]} (interpret-vars-args opts)]
    (spit runner-src-file
          (-> (slurp (io/resource "clj_graal/template/by_var_test_runner.clj.template"))
              (.replace "#_@TEST_NSES_HERE" (string/join "\n" (sort nses)))
              (.replace "#_@TEST_VARS_HERE" (string/join "\n" (sort-by symbol vars)))))
    (println "done")))

(defn- cmd-generate-call-vars-direct[opts]
  (let [{:keys [vars nses runner-src-file]} (interpret-vars-args opts)]
    (spit runner-src-file
          (-> (slurp (io/resource "clj_graal/template/direct_runner.clj.template"))
              (.replace "#_@TEST_NSES_HERE" (string/join "\n" nses))
              (.replace "#_@TEST_FNS_HERE" (string/join "\n"
                                                        (map (fn [ns]
                                                               (str
                                                                "(println \"running: " (symbol ns) " \")\n"
                                                                "(" (symbol ns) ")"))
                                                             vars)))))
    (println "done")))

(def climatic-config
  {:app {:command "gen-test-runner"
         :description "Generate a test-runner that simply runs tests"
         :version "0.0.0"}
   :global-opts [{:option  "dest-dir"
                  :as      "Where to write the generated test-runner.clj"
                  :type    :string
                  :default "target/clj-graal/generated"}]
   :commands [{:command "find-all-vars"
               :description "Return a list of all test vars"
               :runs cmd-find-all-vars}
              {:command "find-all-namespaces"
               :description "Return a list of all test namespaces"
               :runs cmd-find-all-namespaces}

              {:command "test-by-namespace"
               :description "Generate a test runner that tests by namespace via run-tests"
               :opts [{:option  "include"
                       :as      "namespace to include in test-runner, can repeat, if none specified all namespaces are included"
                       :type    :string
                       :multiple true}]
               :runs cmd-generate-test-by-namespace}
              {:command "test-by-var"
               :description "Generate a test runner that runs individual vars via modified test-vars tests"
               :opts [{:option  "include"
                       :as      "var to include in test-runner, can repeat, if none specified all vars are included"
                       :type    :string
                       :multiple true}]
               :runs cmd-generate-test-by-var}
              {:command "call-vars-direct"
               :description "Generate a test runner that calls individual ns/vars functions directly"
               :opts [{:option  "include"
                       :as      "var to include in test-runner, can repeat, if none specified all vars are included"
                       :type    :string
                       :multiple true}]
               :runs cmd-generate-call-vars-direct}
              ]})

(defn -main [& args]
  (cli-matic/run-cmd args climatic-config))
