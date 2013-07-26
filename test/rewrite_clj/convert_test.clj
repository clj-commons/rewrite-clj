(ns ^{ :doc "Tests for Converter."
       :author "Yannick Scherer" }
  rewrite-clj.convert-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.convert :as c]))

(tabular
  (fact "about EDN tree to s-expression conversion"
    (c/->sexpr ?tree) => ?sexpr)
  ?tree                                     ?sexpr
  [:token 0]                                0
  [:token 'id]                              'id
  [:token :k]                               :k
  [:token "s"]                              "s"
  [:token "abc\ndef"]                       "abc\ndef"
  [:multi-line "abc" "def"]                 "abc\ndef"
  [:list [:token :k] [:token 5]]            '(:k 5)
  [:vector [:token :k] [:token 5]]          [:k 5]
  [:set [:token :k] [:token 5]]             #{:k 5}
  [:map [:token :k] [:token 5]]             {:k 5}
  [:eval [:list [:token 'identity]]]        '(identity))

(tabular 
  (fact "about EDN tree to s-expression conversion with expansion"
    (c/->sexpr [?t [:token 'x]]) => (list ?p 'x))
  ?t                ?p
  :quote            'quote
  :unquote          'unquote
  :unquote-splicing 'unquote-splicing
  :syntax-quote     'quote
  :var              'var)

(fact "about EDN tree to s-expression conversion with metadata."
  (let [x (c/->sexpr [:meta [:token :private] [:token 'x]])]
    x => symbol?
    (meta x) => {:private true})
  (let [x (c/->sexpr [:meta* [:token :private] [:token 'x]])]
    x => symbol?
    (meta x) => {:private true}))

(fact "about EDN tree to fn s-expression conversion"
  (let [form (c/->sexpr [:fn [:token '+] [:token '%] [:token 1]])]
    (list? form)
    (first form) => 'fn
    (second form) => vector?
    (let [sym (first (second form))]
      (last form) => (list '+ sym 1))))

(fact "about EDN conversion errors"
  (c/->sexpr [:reader-macro [:token '+clj] [:token 5]]) => (throws Exception)
  (c/->sexpr [:forms [:token 1] [:token 2]]) => (throws Exception))

(tabular
  (fact "about s-expression to EDN tree conversion"
    (c/->tree ?sexpr) => ?tree)
  ?sexpr                                   ?tree
  0                                        [:token 0]
  :k                                       [:token :k]
  'x                                       [:token 'x]
  "s"                                      [:token "s"]
  '(1 2)                                   [:list [:token 1] [:whitespace " "] [:token 2]]
  [1 2]                                    [:vector [:token 1] [:whitespace " "] [:token 2]]
  #{1 2}                                   [:set [:token 1] [:whitespace " "] [:token 2]]
  {:k 2}                                   [:map [:token :k] [:whitespace " "] [:token 2]]
  #'identity                               [:var [:token 'identity]])
