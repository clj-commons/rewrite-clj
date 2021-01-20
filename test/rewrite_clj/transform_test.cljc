(ns rewrite-clj.transform-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [rewrite-clj.zip :as z]))

(def data-string
  (str ";; This is a Project File.\n"
       "(defproject my-project \"0.1.0-SNAPSHOT\"\n"
       "  :description \"A project.\n"
       "                 Multiline!\"\n"
       "  :dependencies [[a \"0.1.0\"]\n"
       "                 [b \"1.2.3\"]]\n"
       "  :repositories { \"private\" \"http://private.com/repo\" })"))

(def data (z/of-string data-string))

(deftest t-a-simple-transformation
  (is (= (string/replace data-string "SNAPSHOT" "SNAPSHOT-1")
         (-> data
             (z/find-value z/next 'defproject)
             z/right z/right
             (z/edit #(str % "-1"))
             z/root-string))))

(deftest t-a-seq-transformation-prefix
  (is (= (-> data-string
             (string/replace "[a " "[prefix-a ")
             (string/replace "[b " "[prefix-b "))
         (-> data
             (z/find-value z/next 'defproject)
             (z/find-value :dependencies) z/right
             (->>
              (z/map
               (fn [loc]
                 (-> loc z/down
                     (z/prefix "prefix-")
                     z/up))))
             z/root-string))))

(deftest t-a-seq-transformation-suffix
  (is (= (-> data-string
             (string/replace "[a " "[a-suffix ")
             (string/replace "[b " "[b-suffix "))
         (-> data
             (z/find-value z/next 'defproject)
             (z/find-value :dependencies) z/right
             (->>
              (z/map
               (fn [loc]
                 (-> loc z/down
                     (z/suffix "-suffix")
                     z/up))))
             z/root-string))))

(deftest t-whitespace-handling-in-removal
  (is (= (string/replace data-string #"\[a [^\s]+\]\s+" "")
         (-> data
             (z/find-value z/next 'defproject)
             (z/find-value :dependencies)
             z/right z/down z/remove
             z/root-string))))
