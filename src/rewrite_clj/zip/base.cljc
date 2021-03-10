(ns ^:no-doc rewrite-clj.zip.base
  (:refer-clojure :exclude [print])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.forms :as nforms]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

(defn get-opts [zloc]
  (:rewrite-clj.zip/opts (meta zloc)))

(defn set-opts [zloc opts]
  (with-meta zloc
    (merge (meta zloc)
           {:rewrite-clj.zip/opts (merge {:auto-resolve node/default-auto-resolve}
                                         opts)})))

;; ## Zipper

(defn edn*
  "Create and return zipper from Clojure/ClojureScript/EDN `node` (likely parsed by [[rewrite-clj.parse]]).

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node]
   (edn* node {}))
  ([node opts]
   (-> (if (:track-position? opts)
         (zraw/custom-zipper node)
         (zraw/zipper node))
       (set-opts opts))))

(defn edn
  "Create and return zipper from Clojure/ClojureScript/EDN `node` (likely parsed by [[rewrite-clj.parse]]),
  and move to the first non-whitespace/non-comment child. If node is not forms node, is wrapped in forms node
  for a consistent root.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (edn node {}))
  ([node opts]
   (loop [node node opts opts]
     (if (= (node/tag node) :forms)
       (let [top (edn* node opts)]
         (or (-> top zraw/down ws/skip-whitespace)
             top))
       (recur (nforms/forms-node [node]) opts)))))

;; ## Inspection

(defn tag
  "Return tag of current node in `zloc`."
  [zloc]
  (some-> zloc zraw/node node/tag))

(defn sexpr-able?
  "Return true if current node in `zloc` can be [[sexpr]]-ed."
  [zloc]
  (some-> zloc zraw/node node/sexpr-able?))

(defn sexpr
  "Return s-expression (the Clojure form) of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc zraw/node (node/sexpr (get-opts zloc)))))

(defn ^{:added "0.4.4"} child-sexprs
  "Return s-expression (the Clojure forms) of children of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc zraw/node (node/child-sexprs (get-opts zloc)))))

(defn length
  "Return length of printable [[string]] of current node in `zloc`."
  [zloc]
  (or (some-> zloc zraw/node node/length) 0))

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED. Return a tag/s-expression pair for inner nodes, or
   the s-expression itself for leaves."
  [zloc]
  (some-> zloc zraw/node node/value))

;; ## Read
(defn of-string
  "Create and return zipper from all forms in Clojure/ClojureScript/EDN string `s`.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([s] (of-string s {}))
  ([s opts]
   (some-> s p/parse-string-all (edn opts))))

#?(:clj
   (defn of-file
     "Create and return zipper from all forms in Clojure/ClojureScript/EDN File `f`.

     Optional `opts` can specify:
     - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
     - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
     ([f] (of-file f {}))
     ([f opts]
      (some-> f p/parse-file-all (edn opts)))))

;; ## Write

(defn ^{:added "0.4.0"} string
  "Return string representing the current node in `zloc`."
  [zloc]
  (some-> zloc zraw/node node/string))

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Renamed to [[string]]."
  [zloc]
  (string zloc))

(defn ^{:added "0.4.0"} root-string
  "Return string representing the zipped-up `zloc` zipper."
  [zloc]
  (some-> zloc zraw/root node/string))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Renamed to [[root-string]]."
  [zloc]
  (root-string zloc))

#?(:clj
   (defn- print! [^String s writer]
     (if writer
       (.write ^java.io.Writer writer s)
       (recur s *out*)))
   :cljs
   (defn- print! [s _writer]
     (string-print s)))

(defn print
  "Print current node in `zloc`.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  ([zloc writer]
   (some-> zloc
           string
           (print! writer)))
  ([zloc] (print zloc nil)))

(defn print-root
  "Zip up and print `zloc` from root node.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  ([zloc writer]
   (some-> zloc
           root-string
           (print! writer)))
  ([zloc] (print-root zloc nil)))
