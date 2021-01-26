(ns code-info.ns-lister
  (:require [cli-matic.core :as cli-matic]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.namespace.find :as find]))

;;
;; to run generated test runner:
;; java -cp "target/clj-graal/generated:$(clojure -M:test-common -Spath)" clojure.main -m clj-graal.test-runner
;;

(defn- nses[lang]
  (->> (find/find-namespaces [(io/file "test")] (if (= lang "cljs")
                                                  find/cljs
                                                  find/clj))
       (filter #(re-matches #".*-test" (str %)))))

;; TODO this will not work for cljs
(defn- vars[test-nses]
  (->> test-nses
       (mapcat (fn [ns]
                 (require ns)
                 (->> (the-ns ns)
                      ns-publics
                      vals
                      (filter #(:test (meta %))))))))

(defn- cmd-find-all-vars[{:keys [lang] :as _opts}]
  (->> (nses lang)
       vars
       (map #(symbol %))
       sort
       (string/join " ")
       println))

(defn- cmd-find-all-namespaces[{:keys [lang] :as _opts}]
  (->> (nses lang)
       sort
       (string/join " ")
       println))

(def climatic-config
  {:app {:command "ns-lister"
         :description "lists test namespaces and tests"
         :version "0.0.0"}
   :global-opts [{:option  "lang"
                  :as      "clj or cljs"
                  :type    :string
                  :default "clj"}]
   :commands [{:command "find-all-vars"
               :description "Return a list of all test vars"
               :runs cmd-find-all-vars}
              {:command "find-all-namespaces"
               :description "Return a list of all test namespaces"
               :runs cmd-find-all-namespaces}]})

(defn -main [& args]
  (cli-matic/run-cmd args climatic-config))
