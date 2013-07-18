(ns ^{ :doc "Print EDN tree."
       :author "Yannick Scherer" }
  rewrite-clj.print)

(defn print-edn
  "Print EDN tree."
  [data]
  (let [[t v] data]
    (condp = t
      :vector (do (print "[") (doseq [x v] (print-edn x)) (print "]"))
      :list (do (print "(") (doseq [x v] (print-edn x)) (print ")"))
      :set (do (print "#{") (doseq [x v] (print-edn x)) (print "}"))
      :map (do (print "{") (doseq [x v] (print-edn x)) (print "}"))
      :meta (do (print "^") (print-edn v))
      :comment (println v)
      :whitespace (print v)
      (pr v))))
