(ns ^:no-doc rewrite-clj.zip.base
  (:refer-clojure :exclude [print])
  (:require [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.protocols :as protocols]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

(defn get-opts [zloc]
  (:rewrite-clj.zip/opts (meta zloc)))

(defn set-opts [zloc opts]
  (with-meta zloc
    (merge (meta zloc)
           {:rewrite-clj.zip/opts (merge {:auto-resolve protocols/default-auto-resolve}
                                         opts)})))

;; ## Zipper

(defn edn*
  "Create zipper over the given Clojure/EDN node.

   If `:track-position?` is set, this will create a custom zipper that will
   return the current row/column using `rewrite-clj.zip/position`. (Note that
   this custom zipper will be incompatible with `clojure.zip`'s functions.)"
  ([node]
   (edn* node {}))
  ([node {:keys [track-position?]}]
   (if track-position?
     (z/custom-zipper node)
     (z/zipper node))))

(defn edn
  "Create and return zipper from Clojure/ClojureScript/EDN `node` (likely parsed by [[rewrite-clj.parse]]),
  and move to the first non-whitespace/non-comment child. If node is not forms node, is wrapped in forms node
  for a consistent root.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-introduction.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([node] (edn node {}))
  ([node opts]
   (-> (loop [node node opts opts]
         (if (= (node/tag node) :forms)
           (let [top (edn* node opts)]
             (or (-> top z/down ws/skip-whitespace)
                 top))
           (recur (node/forms-node [node]) opts)))
       (set-opts opts))))

;; ## Inspection

(defn tag
  "Get tag of node at the current zipper location."
  [zloc]
  (some-> zloc z/node node/tag))

(defn sexpr
  "Return s-expression (the Clojure form) of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-introduction.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc z/node (node/sexpr (get-opts zloc)))))

(defn ^{:added "0.4.4"} child-sexprs
  "Return s-expression (the Clojure forms) of children of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-introduction.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc z/node (node/child-sexprs (get-opts zloc)))))

(defn length
  "Get length of printable string for the given zipper location."
  [zloc]
  (or (some-> zloc z/node node/length) 0))

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED. Return a tag/s-expression pair for inner nodes, or
   the s-expression itself for leaves."
  [zloc]
  (some-> zloc z/node node/value))

;; ## Read

(defn of-string
  "Create zipper from String."
  ([s] (of-string s {}))
  ([s options]
   (some-> s p/parse-string-all (edn options))))

#?(:clj
   (defn of-file
     "Create zipper from File."
     ([f] (of-file f {}))
     ([f options]
      (some-> f p/parse-file-all (edn options)))))

;; ## Write

(defn ^{:added "0.4.0"} string
  "Create string representing the current zipper location."
  [zloc]
  (some-> zloc z/node node/string))

(defn ^{:added "0.4.0"} root-string
  "Create string representing the zipped-up zipper."
  [zloc]
  (some-> zloc z/root node/string))

#?(:clj
   (defn- print! [^String s writer]
     (if writer
       (.write ^java.io.Writer writer s)
       (recur s *out*)))
   :cljs
   (defn- print! [s _writer]
     (string-print s)))

(defn print
  "Print current zipper location.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  [zloc & [writer]]
  (some-> zloc
          string
          (print! writer)))

(defn print-root
  "Zip up and print root node.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  [zloc & [writer]]
  (some-> zloc
          root-string
          (print! writer)))
