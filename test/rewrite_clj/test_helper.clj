(ns rewrite-clj.test-helper
  "Generic test help fns (currently Clojure only, adapt as necessary)"
  (:require [clojure.string :as str])
  (:import [java.io File]))

(defmacro with-test-file
  "Minimal temp file creator, shaped like `let` for easy kondo linting.
  Caller must not delete `temp-file-sym`, file deletion in this macro is part of verification."
  [[temp-file-sym {:keys [prefix suffix]}] & body]
  `(let [~temp-file-sym (File/createTempFile (or ~prefix "rewrite-clj-test") ~suffix)]
     (try
       ~@body
       (finally
         ;; use a delete that will throw on failure
         (java.nio.file.Files/delete (.toPath ~temp-file-sym))))))

(defn os []
  (let [os-name (str/lower-case (System/getProperty "os.name"))]
    (condp re-find os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :linux
      #"sunos" :solaris
      :unknown)))

(defn jdk-major []
  (let [[major minor] (str/split (System/getProperty "java.version") #"\.")
        major (if (= "1" major) minor major)]
    (Integer/parseInt major)))
