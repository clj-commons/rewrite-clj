(ns ^{ :doc "Print EDN tree."
       :author "Yannick Scherer" }
  rewrite-clj.print)

(defmulti print-edn
  (fn [data]
    (when (vector? data)
      (first data)))
  :default :token)

(defmethod print-edn :token [data] (pr (second data)))
(defmethod print-edn :comment [data] (println (second data)))
(defmethod print-edn :whitespace [data] (print (second data)))
(defmethod print-edn :meta [data] (print "^") (doall (map print-edn (rest data))))

(defmethod print-edn :list [data]
  (print "(")
  (doall (map print-edn (rest data)))
  (print ")"))

(defmethod print-edn :vector [data]
  (print "[")
  (doall (map print-edn (rest data)))
  (print "]"))

(defmethod print-edn :set [data]
  (print "#{")
  (doall (map print-edn (rest data)))
  (print "}"))

(defmethod print-edn :map [data]
  (print "{")
  (doall (map print-edn (rest data)))
  (print "}"))

(defmethod print-edn :quote [data]
  (print "'")
  (doall (map print-edn (rest data))))

(defmethod print-edn :syntax-quote [data]
  (print "`")
  (doall (map print-edn (rest data))))

(defmethod print-edn :unquote [data]
  (print "~")
  (doall (map print-edn (rest data))))

(defmethod print-edn :unquote-splicing [data]
  (print "~@")
  (doall (map print-edn (rest data))))
