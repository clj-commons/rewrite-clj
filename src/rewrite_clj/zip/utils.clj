(ns ^{ :doc "Utility operations for nodes."
       :author "Yannick Scherer" }
  rewrite-clj.zip.utils
  (:require [fast-zip.core :as z]))

;; ## Prefix

(defmulti prefix
  "Prefix the value of a zipper node with the given string. This supports multi-lined strings."
  (fn [zloc p]
    (when zloc
      (first (z/node zloc))))
  :default nil)

(defmethod prefix nil
  [zloc prefix]
  (throw (Exception. (str "Cannot prefix value of type: " (first (z/node zloc))))))

(defmethod prefix :token
  [zloc prefix]
  (let [v (second (z/node zloc))
        v1 (cond (string? v) (str prefix v)
                 (symbol? v) (symbol (namespace v) (str prefix (name v)))
                 (keyword? v) (keyword (namespace v) (str prefix (name v)))
                 :else (throw (Exception. (str "Cannot prefix token: " v))))]
    (z/replace zloc [:token v1])))

(defmethod prefix :multi-line
  [zloc prefix]
  (let [[v & rst] (rest (z/node zloc))]
    (z/replace zloc (vec (list* :multi-line (str prefix v) rst)))))

;; ## Suffix

(defmulti suffix
  "Suffix the value of a zipper node with the given string. This supports multi-lined strings."
  (fn [zloc p]
    (when zloc
      (first (z/node zloc))))
  :default nil)

(defmethod suffix nil
  [zloc suffix]
  (throw (Exception. (str "Cannot suffix value of type: " (first (z/node zloc))))))

(defmethod suffix :token
  [zloc suffix]
  (let [v (second (z/node zloc))
        v1 (cond (string? v) (str v suffix)
                 (symbol? v) (symbol (namespace v) (str (name v) suffix))
                 (keyword? v) (keyword (namespace v) (str (name v) suffix))
                 :else (throw (Exception. (str "Cannot suffix token: " v))))]
    (z/replace zloc [:token v1])))

(defmethod suffix :multi-line
  [zloc suffix]
  (let [[v & rst] (rest (z/node zloc))]
    (z/replace zloc (vec (list* :multi-line (str v suffix) rst)))))
