(ns clean
  (:require [babashka.fs :as fs]
            [helper.main :as main]))

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (println "Deleting (d=deleted -=did not exist)")
    (run! (fn [d]
            (println (format "[%s] %s"
                             (if (fs/exists? d) "d" "-")
                             d))
            (fs/delete-tree d {:force true}))
          ["target"
           ".cpcache"
           ".clj-kondo/.cache"
           ".lsp/.cache"
           ".eastwood"
           ".shadow-cljs"])))
