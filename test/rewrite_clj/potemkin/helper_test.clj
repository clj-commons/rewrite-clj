(ns ^:skip-for-sci ;; internal API
    rewrite-clj.potemkin.helper-test
  (:require [clojure.test :refer [deftest is]]
            [rewrite-clj.potemkin.helper :as helper]))

(deftest t-unravel-syms
  (is (= '(one.two.three/a
           one.two.three/b
           sunday.sally/f
           monday.molly/g
           monday.molly/h
           wednesday.willy/i
           thursday.theo/j
           friday.fred/k)
         (helper/unravel-syms [['one.two.three 'a 'b]
                               ['sunday.sally 'f]
                               ['monday.molly 'g 'h]
                               ['wednesday.willy 'i]
                               ['thursday.theo 'j]
                               ['friday.fred 'k]]))))

(deftest t-new-meta-doc-changed
  (is (= {:doc "Orignal sym `original-name` and original doc `original doc`" :name 'original-name :line 234}
         (helper/new-meta
          {:name 'original-name :doc "original doc" :line 234}
          {:doc-to-pattern "Orignal sym `@@orig-name@@` and original doc `@@orig-doc@@`"}))))

(deftest t-new-meta-doc-changed-no-orig-doc
  (is (= {:doc "Orignal sym `original-name` and original doc ``" :name 'original-name :line 234}
         (helper/new-meta
          {:name 'original-name :line 234}
          {:doc-to-pattern "Orignal sym `@@orig-name@@` and original doc `@@orig-doc@@`"}))))

(deftest t-new-meta-doc-unchanged
  (is (= {:name 'original-name :doc "original doc" :line 234}
         (helper/new-meta {:name 'original-name :doc "original doc" :line 234}
                          nil))))
