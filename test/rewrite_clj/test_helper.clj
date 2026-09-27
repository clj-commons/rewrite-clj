(ns rewrite-clj.test-helper
  "Generic test help fns (currently Clojure only, adapt as necessary)"
  (:import [java.io File]
           [java.nio.file Files]))

(defmacro with-test-file
  "Minimal temp file creator, shaped like `let` for easy kondo linting.
  Caller must not delete `temp-file-sym`, file deletion in this macro is part of verification."
  [[temp-file-sym {:keys [prefix suffix]}] & body]
  `(let [~temp-file-sym (File/createTempFile (or ~prefix "rewrite-clj-test") ~suffix)]
     (try
       ~@body
       (finally
         ;; use a delete that will throw on failure
         (Files/delete (.toPath ~temp-file-sym))))))
