(ns rewrite-clj.zip.subedit-test
  (:require [clojure.test :refer [deftest testing is]]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.subedit :as subedit]))

(let [root (base/of-string "[1 #{2 [3 4] 5} 6]")]
  (deftest t-modifying-subtrees
    (let [loc (subedit/subedit-> root
                                 m/next
                                 m/next
                                 m/next
                                 (z/replace 'x))]
      (is (= :vector (base/tag loc)))
      (is (= "[1 #{x [3 4] 5} 6]" (base/string loc)))))
  (deftest t-modifying-the-whole-tree
    (let [loc (subedit/edit-> (-> root m/next m/next m/next)
                              m/prev m/prev
                              (z/replace 'x))]
      (is (= :token (base/tag loc)))
      (is (= "2" (base/string loc)))
      (is (= "[x #{2 [3 4] 5} 6]" (base/root-string loc))))))

(deftest zipper-retains-options
  (let [zloc (base/of-string "(1 (2 (3 4 ::my-kw)))" {:auto-resolve (fn [_x] 'custom-resolved)})
        orig-opts (base/get-opts zloc)]
    (testing "sanity - without subzip"
      (is (= :custom-resolved/my-kw (-> zloc
                                        m/down m/right
                                        m/down m/right
                                        m/down m/rightmost base/sexpr))))
    (testing "subzip"
      (let [sub-zloc (-> zloc z/up subedit/subzip z/down)]
        (is (= orig-opts (base/get-opts sub-zloc)))
        (is (= :custom-resolved/my-kw (-> sub-zloc
                                          m/down m/right
                                          m/down m/right
                                          m/down m/rightmost base/sexpr)))))
    (testing "edit-node"
      (let [edited-zloc (-> zloc (subedit/edit-node
                                  (fn [zloc-edit]
                                    (-> zloc-edit
                                        m/down m/right
                                        m/down (z/replace 'x)))))]
        (is (= 'x (-> edited-zloc m/down m/right m/down base/sexpr)))
        (is (= orig-opts (base/get-opts edited-zloc)))
        (is (= :custom-resolved/my-kw (-> edited-zloc
                                          m/down m/right
                                          m/down m/right
                                          m/down m/rightmost base/sexpr)))))))
