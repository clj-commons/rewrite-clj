(ns ^{ :doc "Find Operations for Zippers"
       :author "Yannick Scherer" }
  rewrite-clj.zip.find
  (:refer-clojure :exclude [find])
  (:require [fast-zip.core :as z]
            [rewrite-clj.zip.core :refer [tag value]]
            [rewrite-clj.zip.move :refer [right]]))

(defn find
  "Find element satisfying the given predicate by applying the given movement function
   to the initial zipper location."
  ([zloc p?] (find zloc right p?))
  ([zloc f p?]
   (->> zloc
     (iterate f)
     (take-while identity)
     (take-while (complement z/end?))
     (drop-while (complement p?))
     (first))))

(defn find-next
  "Find element other than the current zipper location matching the given predicate by 
   applying the given movement function to the initial zipper location."
  ([zloc p?] (find-next zloc right p?))
  ([zloc f p?]
   (when-let [zloc (f zloc)]
     (find zloc f p?))))

(defn find-tag
  "Find element with the given tag by applying the given movement function to the initial 
   zipper location."
  ([zloc t] (find-tag zloc right t))
  ([zloc f t] (find zloc f #(= (tag %) t))))

(defn find-next-tag
  "Find element other than the current zipper location with the given tag by applying the 
   given movement function to the initial zipper location."
  ([zloc t] (find-next-tag zloc right t))
  ([zloc f t] (find-next zloc f #(= (tag %) t))))

(defn find-token
  "Find token element matching the given predicate by applying the given movement function
   to the initial zipper location, defaulting to `right`."
  ([zloc p?] (find-token zloc right p?))
  ([zloc f p?] (find zloc f (fn [x] (and (= (tag x) :token) (p? (value x)))))))

(defn find-next-token
  "Find token element matching the given predicate by applying the given movement function
   to the initial zipper location, defaulting to `right`."
  ([zloc p?] (find-next-token zloc right p?))
  ([zloc f p?] (find-next zloc f (fn [x] (and (= (tag x) :token) (p? (value x)))))))

(defn find-value
  "Find token element whose value matches the given one by applying the given movement
   function to the initial zipper location, defaulting ro `right`."
  ([zloc v] (find-value zloc right v))
  ([zloc f v] (find-token zloc f #(= % v))))

(defn find-next-value
  "Find token element whose value matches the given one by applying the given movement
   function to the initial zipper location, defaulting ro `right`."
  ([zloc v] (find-next-value zloc right v))
  ([zloc f v] (find-next-token zloc f #(= % v))))
