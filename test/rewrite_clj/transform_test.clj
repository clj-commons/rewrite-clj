(ns ^{ :doc "Transformation Tests" 
       :author "Yannick Scherer" }
  rewrite-clj.transform-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]))

(def data-string
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.\"
  :dependencies [[a \"0.1.0\"]
                 [b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(def data (z/of-string data-string))

(fact "about a simple transformation"
  (-> data 
    (z/find-value z/next 'defproject) 
    (z/find-value :description)
    z/right (z/edit #(str "DESCR: " %))
    z/->root-string)
      => 
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"DESCR: A project.\"
  :dependencies [[a \"0.1.0\"]
                 [b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })")

(fact "about a seq transformation"
  (-> data
    (z/find-value z/next 'defproject)
    (z/find-value :dependencies) z/right
    (->>
      (z/map
        (fn [loc]
          (-> loc z/down 
            (z/edit (comp symbol #(str "prefix-" %)) )
            z/up))))
    z/->root-string)
      =>
";; This is a Project File.
(defproject my-project \"0.1.0-SNAPSHOT\"
  :description \"A project.\"
  :dependencies [[prefix-a \"0.1.0\"]
                 [prefix-b \"1.2.3\"]]
  :repositories { \"private\" \"http://private.com/repo\" })" 
      )
