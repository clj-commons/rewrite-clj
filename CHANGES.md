## Changelog

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
  - `insert-right
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
