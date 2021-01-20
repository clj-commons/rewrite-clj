(ns hooks.rewrite-clj.potemkin.import-vars-with-mods
  "clj-kondo has support for potemkin `import-vars`, but does not know about rewrite-clj's
  internal `import-vars-with-mods`."
  (:require [clj-kondo.hooks-api :as api]
            [clojure.string :as string]))


(defn import-vars-with-mods
  "import-vars-with-mods arguments are:
  - option map
  - 1 to n of symbol import definitions

  A symbol import definition is a vector whose
  - first arg is the namespace to import from
  - remaining args are the symbols to import

  Example call with one symbol import definition:
  ```
  (import-vars-with-mods
   {:sym-to-pattern \"@@orig-name@@*\"
    :doc-to-pattern \"Raw version of [[@@orig-name@@]].\n\n@@orig-doc@@\n\nNOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes.\"}
    [rewrite-clj.custom-zipper.core
     right left up down])
  ```

  For the purposes of clj-kondo, we need to look at the `:sym-to-pattern` value in the option
  map, it tells us how to name the local symbol.

  This hook satisfies, (I think), clj-kondo's linting needs by converting the above example call to:
  ```
  (do
    (def right* rewrite-clj.custom-zipper.core/right)
    (def left* rewrite-clj.custom-zipper.core/left)
    (def up* rewrite-clj.custom-zipper.core/up)
    (def down* rewrite-clj.custom-zipper.core/down))

  ```
  It is wrapped in a do because, as far as I understand, I can only return a single node from this hook.

  Reminder: Input and output are rewrite-clj nodes, see clj-kondo docs on hooks."
  [{:keys [:node]}]
  (let [args-node (rest (:children node))
        opts-node (first args-node)
        to-pattern (:sym-to-pattern (api/sexpr opts-node))
        symbol-import-definitions-nodes (rest args-node)
        new-node (api/list-node
                  (conj
                   (doall
                    (for [syms-def-node symbol-import-definitions-nodes
                          from-sym-node (rest (:children syms-def-node))
                          :let [from-ns-node (first (:children syms-def-node))
                                from-sym (api/sexpr from-sym-node)
                                from-sym-fully-qualified (str (api/sexpr from-ns-node) "/" from-sym)
                                to-sym (string/replace to-pattern "@@orig-name@@" (str from-sym))]]
                      (api/list-node
                       [(api/token-node 'def)
                        (api/token-node (symbol to-sym))
                        (api/token-node (symbol from-sym-fully-qualified))])))
                   (api/token-node 'do)))]
    ;; uncomment following print then run clj-kondo to debug
    ;; (prn (api/sexpr new-node))
    {:node new-node}))
