(ns rewrite-clj.transform-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]
            [clojure.string :as string]))

(def data-string
  (str ";; This is a Project File.\n"
       "(defproject my-project \"0.1.0-SNAPSHOT\"\n"
       "  :description \"A project.\n"
       "                 Multiline!\"\n"
       "  :dependencies [[a \"0.1.0\"]\n"
       "                 [b \"1.2.3\"]]\n"
       "  :repositories { \"private\" \"http://private.com/repo\" })"))

(def data (z/of-string data-string))

(fact "about a simple transformation"
      (-> data
        (z/find-value z/next 'defproject)
        z/right z/right
        (z/edit #(str % "-1"))
        z/->root-string)
      => (string/replace data-string "SNAPSHOT" "SNAPSHOT-1"))

(fact "about a seq transformation (prefix)"
      (-> data
        (z/find-value z/next 'defproject)
        (z/find-value :dependencies) z/right
        (->>
          (z/map
            (fn [loc]
              (-> loc z/down
                  (z/prefix "prefix-")
                  z/up))))
        z/->root-string)
      => (-> data-string
           (string/replace "[a " "[prefix-a ")
           (string/replace "[b " "[prefix-b ")))

(fact "about a seq transformation (suffix)"
      (-> data
        (z/find-value z/next 'defproject)
        (z/find-value :dependencies) z/right
        (->>
          (z/map
            (fn [loc]
              (-> loc z/down
                  (z/suffix "-suffix")
                  z/up))))
        z/->root-string)
      => (-> data-string
           (string/replace "[a " "[a-suffix ")
           (string/replace "[b " "[b-suffix ")))

(fact "about whitespace-handling in removal"
      (-> data
        (z/find-value z/next 'defproject)
        (z/find-value :dependencies)
        z/right z/down z/remove
        z/->root-string)
      => (string/replace data-string #"\[a [^\s]+\]\s+" ""))
