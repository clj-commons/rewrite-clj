(ns ^:no-doc rewrite-clj.node.regex
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord RegexNode [pattern]
  node/Node
  (tag [_node] :regex)
  (node-type [_node] :regex)
  (printable-only? [_node] false)
  (sexpr* [_node _opts]
    (list 're-pattern pattern))
  (length [_node]
    (+ 3 ;; 2 double quotes and a hash
       (count pattern)))
  (string [_node]
    (str "#\"" pattern "\""))

  Object
  (toString [node]
    (node/string node)))

(node/make-printable! RegexNode)

;; Internal Utils

(defn pattern-string-for-regex [#?(:clj ^java.util.regex.Pattern regex
                                   :cljs regex)]
  #?(:clj (.pattern regex)
     :cljs (.. regex -source)))

;; ## Constructor

(defn regex-node
  "Create node representing a regex with `pattern-string`.
   Use same escape rules for `pattern-string` as you would for `(re-pattern \"pattern-string\")`

   ```Clojure
   (require '[rewrite-clj.node :as n])

   (-> (n/regex-node \"my\\\\.lil.*regex\")
       n/string)
   ;; => \"#\\\"my\\\\.lil.*regex\\\"\"
   ```"
  [pattern-string]
  (->RegexNode pattern-string))
