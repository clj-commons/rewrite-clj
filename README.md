# rewrite-clj

__rewrite-clj__ is a library offering mechanisms to easily rewrite Clojure/EDN documents in a whitespace- and comment-preserving
way. It includes an EDN parser (based on [clojure.tools.reader](https://github.com/clojure/tools.reader)), a corresponding
printer, as well as an EDN-aware zipper implementation (based on [fast-zip](https://github.com/akhudek/fast-zip)).

[![Build Status](https://travis-ci.org/xsc/rewrite-clj.png?branch=master)](https://travis-ci.org/xsc/rewrite-clj)
[![endorse](https://api.coderwall.com/xsc/endorsecount.png)](https://coderwall.com/xsc)

This project is similar to Christophe Grand's [sjacket](https://github.com/cgrand/sjacket). In fact, I found it hard to _use_
sjacket, partly because of the underlying data structure (a [parsely](https://github.com/cgrand/parsley)-generated tree including
unnecessary data like parentheses and maintaining values as strings instead of their Clojure pendants), but mostly because of the
missing documentation. For those with similar experiences: rewrite-clj is for you!

__This project is in flux. Anything may change at any time.__

## Usage

__Leiningen ([via Clojars](http://clojars.org/rewrite-clj))__

```clojure
[rewrite-clj "0.1.0-SNAPSHOT"]
```

__Parsing Data__

The parser relies on [clojure.tools.reader](https://github.com/clojure/tools.reader) when handling simple
tokens. It generates a structure of nested vectors whose first elements represent the kind of data
contained (`:token`, `:whitespace`, `:comment`, `:list`, ...).

```clojure
(require '[rewrite-clj.parser :as p])
(p/parse-string "(defn my-function [a]\n  (* a 3))")
;; =>
;; [:list 
;;   [:token defn] [:whitespace " "] [:token my-function] [:whitespace " "] 
;;   [:vector [:token a]] [:whitespace "\n  "] 
;;   [:list 
;;     [:token *] [:whitespace " "] [:token a] [:whitespace " "] [:token 3]]]
```

__Printing Data__

The printer incorporates whitespaces and comments in its output.

```clojure
(require '[rewrite-clj.printer :as prn])
(prn/print-edn (p/parse-string "(defn my-function [a]\n  (* a 3))"))
;; (defn my-function [a]
;;   (* a 3))
;; => nil
```

__EDN Zipper__

To traverse/modify the generated structure you can use rewrite-clj's whitespace-/comment-/value-aware zipper
operations, based on [fast-zip](https://github.com/akhudek/fast-zip).

```clojure
(require '[rewrite-clj.zip :as z])
(def data-string 
"(defn my-function [a] 
  ;; a comment
  (* a 3))")
(def data (z/edn (p/parse-string data-string)))

(z/sexpr data)                       ;; => (defn my-function [a] (* a 3))
(-> data z/down z/right z/node)      ;; => [:token my-function]
(-> data z/down z/right z/sexpr)     ;; => my-function

(-> data z/down z/right (z/edit (comp symbol str) "2") z/up z/sexpr)
;; => (defn my-function2 [a] (* a 3))

(-> data z/down z/right (z/edit (comp symbol str) "2") z/root prn/print-edn)
;; (defn my-function2 [a]
;;   ;; a comment
;;   (* a 3))
;; => nil
```

`rewrite-clj.zip/edit` and `rewrite-clj.zip/replace` try to facilitate their use by transparently converting
between the node's internal representation (`[:token my-function]`) and its corresponding s-expression (`my-function`).

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
