(ns sci-test-gen-publics
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [clojure.tools.namespace.find :as find]))

(defn matches-some-pat [spats s]
  (let [pats (map re-pattern spats)]
    (some #(re-matches % s) pats)))

(defn find-nses [{:keys [:dirs :exclude-ns-regexes]}]
  (->> (find/find-namespaces (map io/file dirs) find/clj)
       (remove #(matches-some-pat exclude-ns-regexes (str %)))))

(defn find-pubs [nses {:keys [:exclude-var-regexes]}]
  (->> nses
       (map (fn [ns]
              (require ns)
              [ns (->> (ns-publics ns)
                       vals
                       (map meta)
                       (remove #(matches-some-pat exclude-var-regexes (str (:name %))))
                       (sort-by :name))]))
       (filter #(seq (second %)))
       (into (sorted-map))))

(defn sci-ns-var-name [ns]
  (symbol (string/join "-" (string/split (str ns) #"\."))))

(defn gen-sci-ns-defs [nses]
  (map #(list 'def (sci-ns-var-name %) (list 'sci/create-ns (symbol (str "'" %))))
       nses))

(defn gen-sci-ns-map-code [pubs {:keys [:fn-wrappers]}]
  (->> pubs
       (map (fn [[ns pubs]]
              [(symbol (str "'" ns))
               (->> pubs
                    (map (fn [pub]
                           [(symbol (str "'" (:name pub)))
                            (let [sym (str ns "/" (:name pub))
                                  wrapper (ffirst (filter (fn [[_ pats]] (matches-some-pat pats sym))
                                                          fn-wrappers))]
                              (if wrapper
                                (list (symbol wrapper)
                                      (symbol sym))
                                (list (symbol 'sci/copy-var)
                                      (symbol sym)
                                      (symbol (sci-ns-var-name ns)))))]))
                    (into (sorted-map)))]))
       (into (sorted-map))))

(defn -main [ & _args ]
  (let [opts {:dirs ["src"]
              :exclude-ns-regexes [".*potemkin.*" "rewrite-clj\\.node\\.coercer" ]
              :exclude-var-regexes [".*Node.*"]
              :fn-wrappers {'import/fn-out-to-sci-out [".*/print" ".*/print-root"]}}
        ns-name "lib-under-sci-test"
        pubs (-> (find-nses opts)
                 (find-pubs opts))
        nses (->> (keys pubs)
                  (map #(symbol (str %)))
                  sort)
        code (concat
              (list (list 'ns (symbol ns-name)
                          (concat (list ':require
                                        '[sci.core :as sci]
                                        '[sci-test.import :as import])
                                  nses)))
              (gen-sci-ns-defs nses)
              (list (list 'def 'namespaces (gen-sci-ns-map-code pubs opts))))]
    (binding [pprint/*print-right-margin* 130
              pprint/*print-miser-width* 130]
      (let [out-file (io/file "target/generated/sci-test/src" (str (.replace ns-name "-" "_") ".clj"))]
        (io/make-parents out-file)
        (io/delete-file out-file true)
        (run! #(pprint/pprint % (io/writer out-file :append true)) code)
        (println "Generated:" (str out-file)))))
  (System/exit 0))
