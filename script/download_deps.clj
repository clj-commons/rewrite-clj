(ns download-deps
  (:require [babashka.tasks :as t]))

;; clojure has a -P command, but to bring down all deps we need to specify all aliases
;; bb deps will be brought down just from running bb (which assumedly is how this code is run)

(defn -main [& _args]
  ;; do all the work from build.clj to avoid repeated JVM launch costs
  (t/clojure "-T:build download-deps"))
