## Changelog

### 0.4.2

- fix `:fn` nodes (were `printable-only?` but should actually create an s-sexpression).
- fix `assert-sexpr-count` to not actually create the s-expressions.

### 0.4.1

- fixes infinite loop when trying to read a character.

### 0.4.0

- __BREAKING__ `rewrite-clj.zip.indent` no longer usable.
- __BREAKING__ node creation/edit has stricter preconditions (e.g. `:meta` has to
  contain exactly two non-whitespace forms).
- __BREAKING__ moved to a type/protocol based implementation of nodes.
- fix radix support. (see #13)
- fix handling of spaces between certain forms. (see #7)
- add node constructor functions.
- add `child-sexprs` function.

### 0.3.12

- fix `assoc` on empty map. (see #16)

### 0.3.11

- drop tests for Clojure 1.4.0.
- fix behaviour of `leftmost`.
- upgrade to fast-zip 0.5.2.

### 0.3.10

- fix behaviour of `next` and `end?`.
- fix prewalk.
- add row/column metadata.

### 0.3.9

- add `end?`.
- allow access to children of quoted forms. (see #6)
- fix children lookup for zipper (return `nil` on missing children). (see #5)

### 0.3.8

- add `:uneval` element type (for `#_form` elements).
- fix `estimate-length` for multi-line strings.

### 0.3.7

- fix zipper creation from file.

### 0.3.6

- upgrade dependencies.
- fix file parser (UTF-8 characters were not parsed correctly, see #24@xsc/lein-ancient).

### 0.3.5

- upgrade dependencies.
- cleanup dependency chain.

### 0.3.4

- upgrade dependencies.

### 0.3.3

- Bugfix: parsing of a variety of keywords threw an exception.

### 0.3.2

- Bugfix: `:1.4` and others threw an exception.

### 0.3.1

- added namespaced keywords.

### 0.3.0

- added token type `:newline` to handle linebreak characters.
- `rewrite-clj.zip/edn` wraps everything into `[:forms ...]` node, but the initial location
  is the node passed to it.
- new functions in `rewrite-clj.zip.core`:
  - `length`
  - `move-to-node`
  - `edit->>`, `edit-node`
  - `subedit->`, `subedit->>`, `edit-children`
  - `leftmost?`, `rightmost?`
- new functions in `rewrite-clj.zip.edit`:
  - `splice-or-remove`
  - `prefix`, `suffix` (formerly `rewrite-clj.zip.utils`)
- `rewrite-clj.zip.edit/remove` now handles whitespace appropriately.
- indentation-aware modification functions in `rewrite-clj.zip.indent`:
  - `indent`
  - `indent-children`
  - `replace`
  - `edit`
  - `insert-left`
  - `insert-right`
  - `remove`
  - `splice`
- fast-zip utility functions in `rewrite-clj.zip.utils`

### 0.2.0

- added more expressive error handling to parser.
- added multi-line string handling (node type: `:multi-line`)
- new functions in `rewrite-clj.printer`:
  - `->string`
  - `estimate-length`
- new functions in `rewrite-clj.zip`:
  - `of-string`, `of-file`
  - `print`, `print-root`
  - `->string`, `->root-string`
  - `append-space`, `prepend-space`
  - `append-newline`, `prepend-newline`
  - `right*`, `left*`, ... (delegating to `fast-zip.core/right`, ...)
- new token type `:forms`
- new functions in `rewrite-clj.parser`:
  - `parse-all`
  - `parse-string-all`
  - `parse-file-all`
- zipper utility functions in `rewrite-clj.zip.utils` (able to handle multi-line strings):
  - `prefix`
  - `suffix`

### 0.1.0

- Initial Release
