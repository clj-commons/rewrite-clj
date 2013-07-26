(ns ^{ :doc "Print EDN tree."
       :author "Yannick Scherer" }
  rewrite-clj.printer)

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

(defmethod print-edn :forms [data] (print-children data))
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
(defmethod print-edn :eval [data] (print-children "#=" data))
(defmethod print-edn :reader-macro [data] (print-children "#" data))
(defmethod print-edn :quote [data] (print-children "'" data))
(defmethod print-edn :syntax-quote [data] (print-children "`" data))
(defmethod print-edn :unquote [data] (print-children "~" data))
(defmethod print-edn :unquote-splicing [data] (print-children "~@" data))

(letfn [(print-line [^String s]
          (let [^String s (pr-str s)]
            (print (.substring s 1 (dec (count s))))))] 
  (defmethod print-edn :multi-line [data]
    (print "\"")
    (loop [sq (rest data)]
      (if-not (= (count sq) 1)
        (let [[s0 & rst] sq]
          (print-line s0)
          (println)
          (recur rst))
        (print-line (first sq))))
    (print "\"")))

;; ## Others

(defn ->string
  "Convert EDN tree to String."
  [data]
  (with-out-str (print-edn data)))

;; ## Character Count

(defmulti estimate-length
  "Estimate length of string created from the given data."
  (fn [data]
    (when (vector? data)
      (first data)))
  :default nil)

(defn- estimate-children-length
  [data]
  (reduce #(+ %1 (estimate-length %2)) 0 (rest data)))

(defmethod estimate-length nil [data] (count (->string data)))
(defmethod estimate-length :forms [data] (estimate-children-length data))
(defmethod estimate-length :comment [data] (inc (count (second data))))
(defmethod estimate-length :whitespace [data] (count (second data)))
(defmethod estimate-length :meta [data] (inc (estimate-children-length data)))
(defmethod estimate-length :meta* [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :list [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :vector [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :map [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :set [data] (+ 3 (estimate-children-length data)))
(defmethod estimate-length :var [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :eval [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :deref [data] (inc (estimate-children-length data)))
(defmethod estimate-length :fn [data] (+ 3 (estimate-children-length data)))
(defmethod estimate-length :reader-macro [data] (inc (estimate-children-length data)))
(defmethod estimate-length :quote [data] (inc (estimate-children-length data)))
(defmethod estimate-length :syntax-quote [data] (inc (estimate-children-length data)))
(defmethod estimate-length :unquote [data] (inc (estimate-children-length data)))
(defmethod estimate-length :unquote-splicing [data] (+ 2 (estimate-children-length data)))
(defmethod estimate-length :multi-line [data]
  (let [parts (rest data)]
    (+ 2 (count parts) 
       (reduce
         (fn [sum p]
           (+ sum (count p))) 0 parts))))
