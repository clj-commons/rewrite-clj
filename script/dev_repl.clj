(ns dev-repl
  (:require [babashka.cli :as cli]
            [babashka.process :as process]
            [lread.status-line :as status]))

(def cli-spec {:help {:desc "This usage help"}

               ;; cider nrepl pass through opts
               :host {:ref "<ADDR>"
                      :alias :h
                      :default "127.0.0.1"
                      :desc "Host address"}
               :bind {:ref "<ADDR>"
                      :alias :b
                      :default "127.0.0.1"
                      :desc "Bind address"}
               :port {:ref "<symbols>"
                      :coerce :int
                      :default 0
                      :alias :p
                      :desc "Port, 0 for auto-select"}})

(defn- usage-help[]
  (status/line :head "Usage help")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:host :bind :port :help]})))

(defn- usage-fail [msg]
  (status/line :error msg)
  (usage-help)
  (System/exit 1))

(defn- parse-opts [args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (usage-fail msg))})]
    (when-let [extra-gunk (-> (meta opts) :org.babashka/cli)]
      (usage-fail (str "unrecognized on the command line: " (pr-str extra-gunk))))
    opts))


(defn launch-repl [flavor args]
  (let [opts (parse-opts args)]
    (if (:help opts)
      (usage-help)
      (do (status/line :head "Launching Clojure %s nREPL" (name flavor))
          (process/exec "clj" (str "-M:1.12:test-common:nrepl:nrepl/" (case flavor
                                                                        :cljs "cljs:cljs"
                                                                        :jvm  "jvm"))
                        "-h" (:host opts)
                        "-b" (:bind opts)
                        "-p" (:port opts))))))

;; Entry points
(defn dev-jvm [& args]
  (launch-repl :jvm args))


(defn dev-cljs [& args]
  (launch-repl :cljs args))
