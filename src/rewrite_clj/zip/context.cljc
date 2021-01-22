(ns ^:no-doc rewrite-clj.zip.context
  (:require [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.node.protocols :as protocols]
            [rewrite-clj.zip.seqz :as seqz]
            [rewrite-clj.zip.walk :as walk]))

(defn- is-map-key? [zloc]
  (->> (iterate z/left zloc)
       (take-while identity)
       count
       odd?))

(defn reapply-context
  "Returns `zloc` with namespaced map sexpr context to all symbols and keywords reapplied from current location downward.

  Keywords and symbols:
  * that are keys in a namespaced map will have namespaced map context applied
  * otherwise will have any namespaced map context removed

  You should only need to use this function if:
  * you care about `sexpr` on keywords and symbols
  * and you are moving keywords and symbols from a namespaced map to some other location."
  [zloc]
  (walk/postwalk zloc
                 #(satisfies? protocols/MapQualifiable (z/node %))
                 (fn [zloc]
                   (let [parent (-> zloc z/up z/up)
                         nsmap (when (and parent (seqz/namespaced-map? parent)) parent)]
                     (if (and nsmap (is-map-key? zloc))
                       (z/replace zloc (protocols/map-context-apply (z/node zloc) (first (protocols/children (z/node nsmap)))))
                       (z/replace zloc (protocols/map-context-clear (z/node zloc))))))))
