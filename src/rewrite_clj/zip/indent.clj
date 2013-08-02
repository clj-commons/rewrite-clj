(ns ^{ :doc "Indentation Handling"
       :author "Yannick Scherer"}
  rewrite-clj.zip.indent
  (:require [fast-zip.core :as z]))

;; ## Propagate Identation
;;
;; If a node is changed, multi-line elements to the right of it might have
;; to adjust identation. For example, the following example changes `keyword`
;; to `k`.
;;
;;     (lookup keyword {:one 1
;;                      :two 2})
;;
;; becomes
;;
;;     (lookup k {:one 1
;;                :two 2})
;;
