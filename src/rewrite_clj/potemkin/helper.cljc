(ns ^:no-doc rewrite-clj.potemkin.helper
  (:require [clojure.string :as string]))

#?(:clj (set! *warn-on-reflection* true))

(defn new-name [orig-name opts]
  (if-let [sym-pattern (:sym-to-pattern opts)]
    (symbol (string/replace sym-pattern #"@@orig-name@@" (str orig-name)))
    orig-name))

(defn new-meta [orig-meta opts]
  (if-let [doc-pattern (:doc-to-pattern opts)]
    (assoc orig-meta :doc (-> doc-pattern
                              (string/replace #"@@orig-name@@" (str (:name orig-meta)))
                              (string/replace #"@@orig-doc@@" (or (:doc orig-meta) ""))))
    orig-meta))

(defn- import-type
  [meta-data]
  (cond (:macro meta-data) :macro
        (:arglists meta-data) :fn
        :else :var))

(defn unravel-syms [sym-args]
  (loop [acc []
         rest-args sym-args]
    (if-let [next-arg (first rest-args)]
      (if (sequential? next-arg)
        (recur (apply conj acc (map #(symbol
                                      (str (first next-arg)
                                           (when-let [ns (namespace %)]
                                             (str "." ns)))
                                      (name %))
                                    (rest next-arg)))
               (rest rest-args))
        (recur (conj acc next-arg) (rest rest-args)))
      acc)))


(defn syms->import-data
  [syms resolve-fn meta-fn opts]
  (map
   (fn [sym]
     (let [vr (resolve-fn sym)
           m (meta-fn vr)
           n (:name m)]
       [sym (import-type m) (new-name n opts) (new-meta m opts)]))
   (unravel-syms syms)))
