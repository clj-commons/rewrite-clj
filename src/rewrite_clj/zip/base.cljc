(ns ^:no-doc rewrite-clj.zip.base
  (:refer-clojure :exclude [print])
  (:require [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.forms :as nforms]
            [rewrite-clj.node.protocols :as node]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip.options :as options]
            [rewrite-clj.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Zipper

(defn of-node*
  "Create and return zipper from a rewrite-clj `node` (likely parsed by [[rewrite-clj.parser]]).

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node]
   (of-node* node {}))
  ([node opts]
   (-> (if (:track-position? opts)
         (zraw/custom-zipper node)
         (zraw/zipper node))
       (options/set-opts opts))))

(defn of-node
  "Create and return zipper from a rewrite-clj `node` (likely parsed by [[rewrite-clj.parser]]),
  and move to the first non-whitespace/non-comment child. If node is not forms node, is wrapped in forms node
  for a consistent root.

  Optional `opts` can specify:
  - `:track-position?` set to `true` to enable ones-based row/column tracking, see [docs on position tracking](/doc/01-user-guide.adoc#position-tracking).
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-user-guide.adoc#namespaced-elements)"
  ([node] (of-node node {}))
  ([node opts]
   (loop [node node opts opts]
     (if (= (node/tag node) :forms)
       (let [top (of-node* node opts)]
         (or (-> top zraw/down ws/skip-whitespace)
             top))
       (recur (nforms/forms-node [node]) opts)))))

(defn edn*
  "DEPRECATED. Renamed to [[of-node*]]."
  ([node]
   (edn* node {}))
  ([node opts]
   (of-node* node opts)))

(defn edn
  "DEPRECATED. Renamed to [[of-node]]."
  ([node] (edn node {}))
  ([node opts]
   (of-node node opts)))

;; ## Inspection

(defn tag
  "Return tag of current node in `zloc`."
  [zloc]
  (some-> zloc zraw/node node/tag))

(defn sexpr-able?
  "Return true if current node's element type in `zloc` can be [[sexpr]]-ed.

   See [related docs in user guide](/doc/01-user-guide.adoc#not-all-clojure-is-sexpr-able)"
  [zloc]
  (some-> zloc zraw/node node/sexpr-able?))

(defn sexpr
  "Return s-expression (the Clojure form) of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc zraw/node (node/sexpr (options/get-opts zloc)))))

(defn child-sexprs
  "Return s-expression (the Clojure forms) of children of current node in `zloc`.

  See docs for [sexpr nuances](/doc/01-user-guide.adoc#sexpr-nuances)."
  ([zloc]
   (some-> zloc zraw/node (node/child-sexprs (options/get-opts zloc)))))

(defn length
  "Return length of printable [[string]] of current node in `zloc`."
  [zloc]
  (or (some-> zloc zraw/node node/length) 0))

(defn value
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

(defn string
  "Return string representing the current node in `zloc`."
  [zloc]
  (some-> zloc zraw/node node/string))

(defn ->string
  "DEPRECATED. Renamed to [[string]]."
  [zloc]
  (string zloc))

(defn root-string
  "Return string representing the zipped-up `zloc` zipper."
  [zloc]
  (some-> zloc zraw/root node/string))

(defn ->root-string
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
