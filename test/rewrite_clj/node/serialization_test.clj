(ns rewrite-clj.node.serialization-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.parser :as p]
            [clojure.java.io :as io]))

(def lein-project-dir
  (io/file (System/getProperty "user.dir")))

(defn extension [f]
  (second (re-matches #".*\.([^.]+)$"
                      (.getName f))))

(defn relative-path
  [from to]
  (let [pthfrom (.getAbsolutePath (io/file from))
        pthto    (.getAbsolutePath (io/file to))]
    (when (not (.startsWith pthto pthfrom))
      (throw (new IllegalArgumentException
                  (str \newline
                       pthfrom \newline
                       " and " \newline
                       pthto \newline
                       "do not share the same prefix"))))
    (let [urifrom (new java.net.URI pthfrom)
          urito (new java.net.URI pthto)]
      (.toString (.relativize urifrom urito)))))

(def project-clj-files
  (->> lein-project-dir
       file-seq
       (filter #(= (extension %) "clj"))))

(defn parsed-nodes [f]
  (-> f
      slurp
      p/parse-string))

(defn str-serialize [nodes]
  (binding [*print-dup* true]
    (pr-str nodes)))

(defn check-str-serialization [nodes]
  (= (-> nodes
         str-serialize
         read-string)
     nodes))



(def relative-project-path
  (partial relative-path lein-project-dir))

(facts "Parsed nodes of every source file in this project can be serialized"
  (doseq [f project-clj-files
          :let [rel-pth (relative-project-path f)]]
    (facts (str "of file " rel-pth)
      (let [ps (parsed-nodes f)]
        (check-str-serialization ps) => true))))
