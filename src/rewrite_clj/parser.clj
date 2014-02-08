(ns ^{ :doc "Comment-/Whitespace-preserving EDN parser."}
  rewrite-clj.parser
  (:require [clojure.tools.reader.reader-types :as r]
            [clojure.java.io :as io :only [input-stream file]]
            [rewrite-clj.parser.core :as p :only [parse-next]]))

;; ## Readers

(defn string-reader
  "Create reader for strings."
  [s]
  (r/indexing-push-back-reader (r/string-push-back-reader s)))

(defn file-reader
  "Create reader for files."
  [f]
  (r/indexing-push-back-reader
    (r/input-stream-push-back-reader 
      (io/input-stream (io/file f)))))

;; ## Parse Wrapper

(defn parse
  "Get next EDN tree from Reader."
  [reader]
  (p/parse-next reader nil))

(defn parse-all
  "Parse all forms from reader. Results will be wrapped in `[:forms ...]` if
   more than one form can be read."
  [reader]
  (let [forms (doall
                (->> (repeatedly #(p/parse-next reader nil))
                  (take-while identity)))]
    (if (> (count forms) 1)
      (vec (list* :forms forms))
      (first forms))))

(defn parse-string
  "Get first form from String."
  [s]
  (let [r (string-reader s)]
    (parse r)))

(defn parse-file
  "Get first form from File."
  [f]
  (let [r (file-reader f)]
    (parse r)))

(defn parse-string-all
  "Get all forms from String."
  [s]
  (let [r (string-reader s)]
    (parse-all r)))

(defn parse-file-all
  "Get all forms from File."
  [f]
  (let [r (file-reader f)]
    (parse-all r)))
