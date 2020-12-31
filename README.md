[![Clojars Project](https://img.shields.io/clojars/v/xsc/rewrite-clj.svg)](https://clojars.org/xsc/rewrite-clj)
[![cljdoc badge](https://cljdoc.org/badge/xsc/rewrite-clj)](https://cljdoc.org/d/xsc/rewrite-clj)
[![CircleCI](https://circleci.com/gh/clj-commons/rewrite-clj.svg?style=svg)](https://circleci.com/gh/clj-commons/rewrite-clj)
# rewrite-clj

__rewrite-clj__ is a library offering mechanisms to easily rewrite Clojure/EDN documents in a whitespace- and comment-preserving
way. It includes an EDN parser (based on [clojure.tools.reader](https://github.com/clojure/tools.reader)), a corresponding
printer, as well as an EDN-aware zipper implementation (based on [clojure.zip](https://clojure.github.io/clojure/clojure.zip-api.html)).

This project is similar to Christophe Grand's [sjacket](https://github.com/cgrand/sjacket). In fact, I found it hard to _use_
sjacket, partly because of the underlying data structure (a [parsely](https://github.com/cgrand/parsley)-generated tree including
unnecessary data like parentheses and maintaining values as strings instead of their Clojure pendants), but mostly because of the
missing documentation. For those with similar experiences: rewrite-clj is for you!

## Usage

__Leiningen ([via Clojars](http://clojars.org/rewrite-clj))__

[![Clojars Project](http://clojars.org/rewrite-clj/latest-version.svg)](http://clojars.org/rewrite-clj)

Auto-generated documentation can be found [here](http://xsc.github.io/rewrite-clj/).

### Parsing Data

The parser relies on [clojure.tools.reader](https://github.com/clojure/tools.reader) when
handling simple tokens and generates a custom node type representing EDN forms:

```clojure
(require '[rewrite-clj.parser :as p])

(def form (p/parse-string "(defn my-function [a]\n  (* a 3))"))
;; => <list:
;;      (defn my-function [a]
;;        (* a 3))
;;    >
```

These nodes can be analysed using functions in `rewrite-clj.node`:

```clojure
(require '[rewrite-clj.node :as n])

(n/tag form)          ;; => :list
(n/children form)     ;; => (<token: defn> <whitespace: " "> <token: my-function> ...)
(n/sexpr form)        ;; => (defn my-function [a] (* a 3))
(n/child-sexprs form) ;; => (defn my-function [a] (* a 3))
```

To convert the structure back to a printable string, use:

```clojure
(n/string form) ;; => "(defn my-function [a]\n  (* a 3))"
```

You can create a node from nearly any value using `coerce`:

```
(n/coerce '[a b c]) ;; => <vector: [a b c]>
```

Alternatively, by hand:

```
(n/meta-node
  (n/token-node :private)
  (n/token-node 'sym))
;; => <meta: ^:private sym>
```

See the [auto-generated documentation](http://xsc.github.io/rewrite-clj/rewrite-clj.node.html) for more information.

### Clojure Zipper

To traverse/modify the generated structure you can use rewrite-clj's
whitespace-/comment-/value-aware zipper operations.

```clojure
(require '[rewrite-clj.zip :as z])
(def data-string
"(defn my-function [a]
  ;; a comment
  (* a 3))")
(def data (z/of-string data-string))

(z/sexpr data)                       ;; => (defn my-function [a] (* a 3))
(-> data z/down z/right z/node)      ;; => <token: my-function>
(-> data z/down z/right z/sexpr)     ;; => my-function

(-> data z/down z/right (z/edit (comp symbol str) "2") z/up z/sexpr)
;; => (defn my-function2 [a] (* a 3))

(-> data z/down z/right (z/edit (comp symbol str) "2") z/print-root)
;; (defn my-function2 [a]
;;   ;; a comment
;;   (* a 3))
;; => nil
```

`rewrite-clj.zip/edit` and `rewrite-clj.zip/replace` try to facilitate their use
by transparently coercing between the node's internal representation (`<token: my-function>`)
and its corresponding s-expression (`my-function`).

See the [auto-generated documentation](http://xsc.github.io/rewrite-clj/rewrite-clj.zip.html) for more information.

## Sweet Code Traversal

### Example

`rewrite-clj.zip` offers a series of `find` operations that can be used to determine specific
positions in the code. For example, you might want to modify a `project.clj` of the following
form by replacing the `:description` placeholder text with something meaningful:

```clojure
(defproject my-project "0.1.0-SNAPSHOT"
  :description "Enter description"
  ...)
```

Most find operations take an optional movement function as parameter. If you wanted to perform
a depth-first search you'd use `rewrite-clj.zip/next`, if you wanted to look for something on
the same level as the current location, you'd employ `rewrite-clj.zip/right` (the default) or
`rewrite-clj.zip/left`.

Now, to enter the project map, you'd look for the symbol `defproject` in a depth-first way:

```clojure
(def data (z/of-file "project.clj"))
(def prj-map (z/find-value data z/next 'defproject))
```

The `:description` keyword should be on the same layer, the corresponding string right of it:

```clojure
(def descr (-> prj-map (z/find-value :description) z/right))
(z/sexpr descr) ;; => "Enter description"
```

Replace it, zip up and print the result:

```clojure
(-> descr (z/replace "My first Project.") z/print-root)
;; (defproject my-project "0.1.0-SNAPSHOT"
;;   :description "My first Project."
;;   ...)
;; => nil
```

See the [auto-generated documentation](http://xsc.github.io/rewrite-clj/) for more information.

### Handling Clojure Data Structures

rewrite-clj aims at providing easy ways to work with Clojure data structures. It offers
functions corresponding to the standard seq functions designed to work with zipper nodes
containing said structures, e.g.:

```clojure
(def data (z/of-string "[1\n2\n3]"))

(z/vector? data)                ;; => true
(z/sexpr data)                  ;; => [1 2 3]
(-> data (z/get 1) z/node)      ;; => #<token: 2>
(-> data (z/assoc 1 5) z/sexpr) ;; => [1 5 3]

(->> data (z/map #(z/edit % + 4)) z/->root-string)
;; => "[5\n6\n7]"
```

See the [auto-generated documentation](http://xsc.github.io/rewrite-clj/) for more information.

## License

```
MIT License

Copyright (c) 2013-2018 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
