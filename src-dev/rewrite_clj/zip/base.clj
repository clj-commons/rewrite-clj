(ns rewrite-clj.zip.base
  (:refer-clojure :exclude [print])
  (:require [rewrite-clj
             [node :as node]
             [parser :as p]]
            [rewrite-clj.zip.whitespace :as ws]
            [fast-zip.core :as z]))

;; ## Zipper

(defn edn*
  "Create zipper over the given Clojure/EDN node."
  [node]
  (z/zipper
    node/inner?
    node/children
    node/replace-children
    node))

(defn edn
  "Create zipper over the given Clojure/EDN node and move
   to the first non-whitespace/non-comment child."
  [node]
  (if (= (node/tag node) :forms)
    (-> (edn* node)
        (z/down)
        (ws/skip-whitespace))
    (recur (node/forms-node [node]))))

;; ## Inspection

(defn tag
  "Get tag of node at the current zipper location."
  [zloc]
  (some-> zloc z/node node/tag))

(defn sexpr
  "Get sexpr represented by the given node."
  [zloc]
  (some-> zloc z/node node/sexpr))

(defn length
  "Get length of printable string for the given zipper location."
  [zloc]
  (or (some-> zloc z/node node/length) 0))

;; ## Read

(defn of-string
  "Create zipper from String."
  [s]
  (some-> s p/parse-string-all edn))

(defn of-file
  "Create zipper from File."
  [f]
  (some-> f p/parse-file-all edn))

;; ## Write

(defn string
  "Create string representing the current zipper location."
  [zloc]
  (some-> zloc z/node node/string))

(defn root-string
  "Create string representing the zipped-up zipper."
  [zloc]
  (some-> zloc z/root node/string))

(defn- print!
  [^String s writer]
  (if writer
    (.write ^java.io.Writer writer s)
    (recur s *out*)))

(defn print
  "Print current zipper location."
  [zloc & [writer]]
  (some-> zloc
          string
          (print! writer)))

(defn print-root
  "Zip up and print root node."
  [zloc & [writer]]
  (some-> zloc
          root-string
          (print! writer)))
