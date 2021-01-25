(ns sci-test-runner
  "Sci interpreted test runner, grabbed concepts I needed from cognitect.test-runner.
  TODO: consider moving more of this under JVM and out of sci interpreted world."
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [clojure.tools.namespace.find :as find]
            [sci-test.test-runner :as test-runner]))

(defn meta-test [v {:keys [include-with-meta exclude-with-meta]}]
  (and (if include-with-meta
         ((apply some-fn include-with-meta) (meta v))
         true)
       (if exclude-with-meta
         ((complement (apply some-fn exclude-with-meta)) (meta v))
         true)))

(defn ns-filter [{:keys [namespace namespace-regex] :as opts}]
  (let [regexes (or namespace-regex [#".*\-test$"])]
    (fn [ns]
      (if namespace
        (namespace ns)
        (and (some #(re-matches % (name ns)) regexes)
             (meta-test ns opts))))))

(defn var-filter [opts]
  (fn [v]
    (meta-test v opts)))

(defn find-test-nses [opts]
  (->> (find/find-namespaces (into [] (map io/file (:dirs opts))) find/clj)
       (filter (ns-filter opts))))

(defn find-test-vars [test-nses opts]
  (->> test-nses
       (mapcat (fn [ns] (->> ns
                             ns-publics
                             vals
                             (filter #(get (meta %) :test))
                             (filter (var-filter opts)))))))

(defn -main[]
  (let [opts {:exclude-with-meta [:skip-for-sci]
              :dirs ["test"]}
        nses (find-test-nses opts)]
    (println "Running tests with options:" opts)
    (dorun (map require nses))
    (let [test-vars (find-test-vars nses opts)
          summary (test-runner/run-test-vars test-vars)]
      (System/exit (if (t/successful? summary) 0 1)))))

(-main)
