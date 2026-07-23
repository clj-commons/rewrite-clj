(ns dev-repl
  (:require [babashka.process :as process]
            [clojure.string :as str]
            [helper.clojure-versions :as clojure-versions]
            [lread.status-line :as status]))

(defn- launch-repl [flavor {:keys [flowstorm host bind port]}]
  (let [aliases (cond-> [(case flavor
                           :cljs "nrepl/cljs:cljs"
                           :jvm  "nrepl/jvm")]
                  flowstorm (conj "flowstorm"))]
    (status/line :head "Launching Clojure %s nREPL" (name flavor))
    (when flowstorm
      (status/line :detail "Flowstorm support is enabled"))
    (process/exec "clj" (str "-M:" (:alias (clojure-versions/current-prod)) ":test-common:nrepl:" (str/join ":" aliases))
                  "-h" host
                  "-b" bind
                  "-p" port)))

;; Entry points
(defn dev-jvm
  {:org.babashka/cli
   {:restrict true :restrict-args true
    :spec {:flowstorm {:alias :f
                       :coerce :boolean
                       :desc "Enable flowstorm"}
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
                  :desc "Port, 0 for auto-select"}}}}
  [opts]
  (launch-repl :jvm opts))

(defn dev-cljs
  ;; TODO: Exact dupe of above, can I DRY this?
  {:org.babashka/cli
   {:restrict true :restrict-args true
    :spec {:flowstorm {:alias :f
                       :coerce :boolean
                       :desc "Enable flowstorm"}
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
                  :desc "Port, 0 for auto-select"}}}}
  [opts]
  (launch-repl :cljs opts))
