(ns helper.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as string] ))

(defn at-path [path prog-name]
  (let [f (io/file path prog-name)]
    (when (and (.isFile f) (.canExecute f))
      (str (.getAbsolutePath f)))))

(defn delete-file-recursively
  [f & [silently]]
  (let [f (io/file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)))

(defn split-path-list [path-list]
  (string/split path-list
                (re-pattern (str "\\" java.io.File/pathSeparator))) )

(defn on-path [prog-name]
  (first (keep identity
               (map #(at-path % prog-name)
                    (split-path-list (System/getenv "PATH"))))))
