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

(defn parse-string
  "Get EDN tree from String."
  [s]
  (let [r (string-reader s)]
    (p/parse-next r nil)))

(defn parse-file
  "Get EDN tree from File."
  [f]
  (let [r (file-reader f)]
    (p/parse-next r nil)))
