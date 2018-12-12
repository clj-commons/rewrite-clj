(ns ^:no-doc rewrite-clj.parser.namespaced-map
  (:require [rewrite-clj
             [node :as node]
             [reader :as reader]]
            [rewrite-clj.parser.utils :as u]))

(defn- parse-nspaced-map-type
  [reader]
  (case (reader/peek reader)
    nil (u/throw-reader reader "Unexpected EOF.")
    \:  (do (reader/ignore reader) "::")
    ":"))

(defn- nspace?
  [reader]
  (let [c (reader/peek reader)]
    (and (not (reader/whitespace? c))
         (not (= \{ c)))))

(defn- keywordize-nspace [type nspace]
  (node/token-node
   (if (= "::" type)
     (keyword (str ":" (node/string nspace)))
     (keyword (node/string nspace)))))

(defn- parse-nspace
  [reader fn-parse-next type]
  (let [n (fn-parse-next reader)]
    (cond
      (nil? n)
      (u/throw-reader reader "Unexpected EOF.")

      (not= :token (node/tag n))
      (if (= "::" type)
        (u/throw-reader reader ":namespaced-map expected namespace alias or map")
        (u/throw-reader reader ":namespaced-map expected namespace prefix"))
      :else (keywordize-nspace type n))))

(defn- parse-for-printable
  [reader fn-parse-next]
  (loop [vs []]
    (if (and
         (seq vs)
         (not (node/printable-only? (last vs))))
      vs
      (if-let [v (fn-parse-next reader)]
        (recur (conj vs v))
        nil))))

(defn- parse-for-map
  [reader fn-parse-next]
  (let [m (parse-for-printable reader fn-parse-next)]
    (cond
      (nil? m)
      (u/throw-reader reader "Unexpected EOF.")

      (not= :map (node/tag (last m)))
      (u/throw-reader reader ":namespaced-map expects a map")
      :else m)))

(defn parse-namespaced-map
  [reader fn-parse-next]
  (reader/ignore reader)
  (let [type (parse-nspaced-map-type reader)]
    (if (nspace? reader)
      (node/namespaced-map-node
       (into [(parse-nspace reader fn-parse-next type)]
             (parse-for-map reader fn-parse-next)))
      (if (= "::" type)
        (node/namespaced-map-node
         (into [(node/token-node (keyword ":"))]
               (parse-for-map reader fn-parse-next)))
        (u/throw-reader reader ":namespaced-map expected namespace prefix")))))
