(ns rewrite-clj
  "APIs to navigate and update Clojure/ClojureScript/EDN source code.

  Use [[rewrite-clj.zip]] to ingest your source code into a zipper of nodes and then again to navigate and/or change it.

  Optionally use [[rewrite-clj.parser]] to instead work with raw nodes.

  [[rewrite-clj.node]] will help you to inspect and create nodes.

  [[rewrite-clj.paredit]] first appeared in the ClojureScript only version of rewrite-clj and supports structured editing of the zipper tree.")
