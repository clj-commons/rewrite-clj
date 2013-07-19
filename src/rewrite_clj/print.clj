(ns ^{ :doc "Print EDN tree."
       :author "Yannick Scherer" }
  rewrite-clj.print)

;; ## Base

(defmulti print-edn
  (fn [data]
    (when (vector? data)
      (first data)))
  :default :token)

(defn- print-children
  ([data] (print-children nil data nil))
  ([prefix data] (print-children prefix data nil))
  ([prefix data suffix]
   (when prefix (print prefix))
   (doall (map print-edn (rest data)))
   (when suffix (print suffix))))

;; ## Printers

(defmethod print-edn :token [data] (pr (second data)))
(defmethod print-edn :comment [data] (println (second data)))
(defmethod print-edn :whitespace [data] (print (second data)))
(defmethod print-edn :meta [data] (print-children "^" data))
(defmethod print-edn :meta* [data] (print-children "#^" data))
(defmethod print-edn :deref [data] (print-children "@" data))
(defmethod print-edn :var [data] (print-children "#'" data))
(defmethod print-edn :fn [data] (print-children "#(" data ")"))
(defmethod print-edn :list [data] (print-children "(" data ")"))
(defmethod print-edn :vector [data] (print-children "[" data "]"))
(defmethod print-edn :map [data] (print-children "{" data "}"))
(defmethod print-edn :set [data] (print-children "#{" data "}"))
(defmethod print-edn :quote [data] (print-children "'" data))
(defmethod print-edn :syntax-quote [data] (print-children "`" data))
(defmethod print-edn :unquote [data] (print-children "~" data))
(defmethod print-edn :unquote-splicing [data] (print-children "~@" data))
