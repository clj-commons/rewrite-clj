;; Run via clojure -X:
;; invoked by apply_import_vars.clj bb script
(ns lread.apply-import-vars
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]
            ;; use internal nses instead of exported nses, we are generating exported nses.
            [rewrite-clj.custom-zipper.core :as zraw]
            [rewrite-clj.node.coercer] ;; to load coercions
            [rewrite-clj.node.comment :as ncomment]
            [rewrite-clj.node.meta :as nmeta]
            [rewrite-clj.node.quote :as nquote]
            [rewrite-clj.node.seq :as nseq]
            [rewrite-clj.node.stringz :as nstring]
            [rewrite-clj.node.token :as ntoken]
            [rewrite-clj.node.whitespace :as nws]
            [rewrite-clj.zip.base :as zbase]
            [rewrite-clj.zip.move :as zmove]
            [rewrite-clj.zip.removez :as zremove]
            [rewrite-clj.zip.walk :as zwalk]))

(defn errors [schema value]
  (-> schema
      (mu/closed-schema)
      (m/explain value)
      (me/with-spell-checking)
      (me/humanize)))

(defn- find-templates []
  (->> (io/file "./template")
       file-seq
       (filter #(and (.isFile %) (re-matches #".*\.clj[sc]?" (str %))))
       sort))

(defn- variadic? [arglist]
  (= '& (-> arglist butlast last)))

(defn- args-from [arglist]
  (remove #(= '& %) arglist))

(defn- gen-fn-call [fn-sym arglist]
  (if (variadic? arglist)
    (concat (list 'apply fn-sym)
            (args-from arglist))
    (concat (list fn-sym) arglist)))

(defn- gen-macro-call [macro-sym arglist]
  (nquote/syntax-quote-node
   [(nseq/list-node
     (let [args (args-from arglist)
           fixed-args (if (variadic? arglist) (butlast args) args)
           variadic (when (variadic? arglist) (last args))
           arg-nodes (mapcat (fn [a] [(nws/spaces 1) (nquote/unquote-node a)]) fixed-args)
           arg-nodes (if variadic
                       (concat arg-nodes [(nws/spaces 1) (nquote/unquote-splicing-node variadic)])
                       arg-nodes)]
       (concat [(ntoken/token-node macro-sym)]
               arg-nodes)))]))

(defn- import-vars-directive? [zloc]
  (and (= :uneval (zbase/tag zloc))
       (= :map  (-> zloc zmove/down zbase/tag))
       (= :import-vars/import (-> zloc zmove/down zmove/down zbase/sexpr))))

(defn- call-sym [var-metadata]
  (symbol (str (ns-name (:ns var-metadata)))
          (str (:name var-metadata))))

(defn- syms-to-import [import-vecs]
  (for [[ns-sym & ns-vars] import-vecs
        ns-var ns-vars]
    (symbol (str ns-sym) (str ns-var))))

(defn- imported-var-name [var-metadata opts]
  (if-let [sym-pattern (:sym-to-pattern opts)]
    (symbol (string/replace sym-pattern #"@@orig-name@@" (str (:name var-metadata))))
    (:name var-metadata)))

(defn- imported-docstring [var-metadata opts]
  (if-let [doc-pattern (:doc-to-pattern opts)]
    (-> doc-pattern
        (string/replace #"@@orig-name@@" (str (:name var-metadata)))
        (string/replace #"@@orig-doc@@" (or (:doc var-metadata) "")))
    (:doc var-metadata)))

(defn- sym-meta [ctx sym]
  (if-let [v (try (requiring-resolve sym)
               (catch Throwable _e))]
    (meta v)
    (throw (ex-info (str "apply-import-vars: unable to resolve: " sym "\n" ctx) {}))))

(defn import-vec-errors [imports]
  ;; not sure how to express this in malli yet... so, roll my own for now
  (or (when-not (and (vector? imports) (seq imports) (every? vector? imports))
        "expected :from to be a vector of vectors")
      (let [non-symbols (into [] (filter (complement symbol?) (flatten imports)))]
        (when (seq non-symbols)
          (format "expected :from to be expressed as symbols, found: %s" non-symbols)))
      (let [bad-sym-counts (into [] (filter #(< (count %) 2) imports))]
        (when (seq bad-sym-counts)
          (format "expected at least 2 symbols per import vector in :from, found: %s" bad-sym-counts)))))

(defn- assert-import-request-valid [ctx import-request]
  (when-let [errors (or (errors [:map {:closed true}
                                  [:opts {:optional true} [:map
                                                           [:sym-to-pattern {:optional true} string?]
                                                           [:doc-to-pattern {:optional true} string?]]]
                                  [:from any?]]
                                 import-request)
                        (import-vec-errors (:from import-request)))]
    (throw (ex-info (format "apply-import-vars: invalid import request: %s\n%s" errors ctx) {}))))

(defn- assert-sym-meta [ctx var-meta]
  (doseq [al (:arglists var-meta)]
    (when (not (every? symbol? al))
      (throw (ex-info (format "appply-import-vars: args expected to be symbols only (no destructuring), found: %s\nfor var: %s\nin ns: %s\n%s" 
                              al (:name var-meta) (:ns var-meta) ctx) {})))))

(defn- delegator-nodes [var-meta]
  (for [al (:arglists var-meta)
        :let [call-to-sym (call-sym var-meta)]]
    (list al
          (nws/spaces 1)
          (if (:macro var-meta)
            (gen-macro-call call-to-sym al)
            (gen-fn-call call-to-sym al)))))

(defn- import-vars* [f zloc {:keys [from opts] :as import-request}]
  (let [ctx (merge {:filename f}
                   (select-keys (-> zloc zraw/node meta) [:row :col]))]
    (assert-import-request-valid ctx import-request)
    (->> (syms-to-import from)
         (reduce (fn [zloc import-sym]
                   (let [vm (sym-meta ctx import-sym)
                         _ (assert-sym-meta ctx vm)
                         delegators (delegator-nodes vm)]
                     (-> zloc
                         (zraw/insert-left (nws/newlines 1))
                         (zraw/insert-left (ncomment/comment-node
                                            (str "; DO NOT EDIT FILE, automatically imported from: "
                                                 (ns-name (:ns vm)))))
                         (zraw/insert-left (nws/newlines 1))
                         (zraw/insert-left (nseq/list-node
                                            (concat (list (if (:macro vm) 'defmacro 'defn)
                                                          (nws/spaces 1)
                                                          (let [target-meta (select-keys vm [:deprecated :added])
                                                                name (imported-var-name vm opts)]
                                                            (if (seq target-meta)
                                                              (nmeta/meta-node target-meta name)
                                                              name))
                                                          (nws/newlines 1)
                                                          (nws/spaces 2)
                                                          ;; mimic what parser does 
                                                          (nstring/string-node (some->> (imported-docstring vm opts)
                                                                                        string/split-lines
                                                                                        (map #(string/replace % "\"" "\\\""))
                                                                                        (into []))))
                                                    (if (= 1 (count delegators))
                                                      (concat [(nws/newlines 1) (nws/spaces 2)]
                                                              (first delegators))
                                                      (mapcat #(list (nws/newlines 1) (nws/spaces 2)
                                                                     (nseq/list-node %))
                                                              delegators)))))
                         (zraw/insert-left (nws/newlines 1)))))
                 zloc)
         (zremove/remove))))

(defn- import-vars [f zloc]
  (let [import-request (-> zloc zmove/down zmove/down zmove/right zbase/sexpr) ]
    (import-vars* f zloc import-request)))

(defn- process-template [f]
  (-> f
      zbase/of-file
      zmove/up
      (zraw/insert-child (ncomment/comment-node (str "; DO NOT EDIT FILE, automatically generated from: " f "\n")))
      (zwalk/prewalk import-vars-directive?
                     (fn visit [zloc]
                       (let [operation (-> zloc zmove/down zmove/down zbase/sexpr)]
                         (case operation
                           ;; only 1 currently...
                           :import-vars/import (import-vars (str f) zloc)))))
      zbase/root-string))

(defn- process-templates []
  (map (fn [t]
         (let [new-target-clj (process-template t)
               template-filename (str t)
               target-filename (string/replace-first template-filename #"^\./template/" "./src/")
               current-target-clj (when (.exists (io/file target-filename)) (slurp target-filename))]
           {:template-filename template-filename
            :target-clj new-target-clj
            :target-filename target-filename
            :changed? (not= new-target-clj current-target-clj)}))
       (find-templates)))

;; entry points for clojure tools cli for -X calls

(defn check [_kwargs]
  (let [templates (process-templates)
        stale-cnt (->> (process-templates)
                       (reduce (fn [stale-cnt {:keys [template-filename target-filename changed?]}]
                                 (println "Template:" template-filename)
                                 (println (if changed? "✗" "✓") "Target:" target-filename (if changed? "STALE" "(no changes)"))
                                 (if changed? (inc stale-cnt) stale-cnt))
                               0))]
    (println (format "\n%d of %d targets are stale." stale-cnt (count templates)))
    (System/exit (if (zero? stale-cnt) 0 1))))

(defn gen-code [_kwargs]
  (let [templates (process-templates)
        update-cnt (->> templates
                        (reduce (fn [update-cnt {:keys [template-filename target-filename changed? target-clj]}]
                                  (println "Template:" template-filename)
                                  (println "  Target:" target-filename (if changed? "UPDATE" "(no changes detected)"))
                                  (if changed?
                                    (do
                                      (spit target-filename target-clj)
                                      (inc update-cnt))
                                    update-cnt))
                                0))]
      (println (format "\n%d of %d targets were updated." update-cnt (count templates)))))


