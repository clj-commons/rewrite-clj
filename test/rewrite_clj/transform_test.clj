(ns ^{ :doc "Transformation Tests" 
       :author "Yannick Scherer" }
  rewrite-clj.transform-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]
            [rewrite-clj.zip.utils :as zu]))

(def data-string
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.
                 Multiline!\"
  :dependencies [[a \"0.1.0\"]
                 [b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(def data (z/of-string data-string))

(fact "about a simple transformation"
  (-> data 
    (z/find-value z/next 'defproject) 
    z/right z/right
    (z/edit #(str % "-1"))
    z/->root-string)
      => 
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT-1\"
  :description \"A project.
                 Multiline!\"
  :dependencies [[a \"0.1.0\"]
                 [b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(fact "about a seq transformation (prefix)"
  (-> data
    (z/find-value z/next 'defproject)
    (z/find-value :dependencies) z/right
    (->>
      (z/map
        (fn [loc]
          (-> loc z/down 
            (zu/prefix "prefix-")
            z/up))))
    z/->root-string)
      =>
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.
                 Multiline!\"
  :dependencies [[prefix-a \"0.1.0\"]
                 [prefix-b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(fact "about a seq transformation (suffix)"
  (-> data
    (z/find-value z/next 'defproject)
    (z/find-value :dependencies) z/right
    (->>
      (z/map
        (fn [loc]
          (-> loc z/down 
            (zu/suffix "-suffix")
            z/up))))
    z/->root-string)
      =>
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.
                 Multiline!\"
  :dependencies [[a-suffix \"0.1.0\"]
                 [b-suffix \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })" 
      )
