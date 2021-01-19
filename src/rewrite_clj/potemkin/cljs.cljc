(ns ^:no-doc rewrite-clj.potemkin.cljs
  (:require [cljs.analyzer :as ana]
            [cljs.analyzer.api :as ana-api]
            [cljs.env :as env]
            [rewrite-clj.potemkin.helper :as helper]))

#?(:clj (set! *warn-on-reflection* true))

;; Strongly based on code from:
;;
;; --- copied from ztellman/potemkin
;;
;; Copyright (c) 2013 Zachary Tellman
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files  (the  "Software"), to
;; deal in the Software without restriction, including without limitation the
;; rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
;; sell copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED  "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
;; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
;; IN THE SOFTWARE.
;;
;; ---

;; --- potemkin.namespaces

(defn- meta-from-resolved
  "mimic what meta would return on a resolved sym in clj"
  [sym-analysis]
  (let [sym-meta (:meta sym-analysis)
        macro (:macro sym-analysis)
        new-meta (assoc sym-meta
             ;; name is not present under meta, add it to mimic clj meta
             :name (symbol (name (:name sym-analysis)))
             ;; doc is not always under meta (ex. for def)
             :doc (get-in sym-analysis [:meta :doc] (:doc sym-analysis)))]
    ;; shadow-cljs does not include :meta for macros, but everyone includes :macro at root
    (if (and (not sym-meta) macro)
      (assoc new-meta :macro macro)
      new-meta)))

(defn- resolve-sym [sym]
  (or (ana-api/resolve @env/*compiler* sym)
      (throw (ex-info (str "potemkin cljs does not recognize symbol: " sym) {:symbol sym}))))

(defn- adjust-var-meta! [target-ns target-name src-sym]
  (let [src-ns (symbol (namespace src-sym))
        src-name (symbol (name src-sym))
        target-ns (symbol (str target-ns))
        src-data (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])]
    (and (or (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])
             (throw (ex-info "adjust-var-meta! did not find metadata for source symbol" {:ns src-ns :name src-name})))
         (or (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name])
             (throw (ex-info "adjust-var-meta! did not find metadata for target symbol" {:ns target-ns :name target-name})))
         (do
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name]
                  merge (dissoc src-data :name :doc))
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name :meta]
                  merge (dissoc (:meta src-data) :name :doc)))))
  nil)

(defmacro ^:private fixup-vars
  "We can't alter-meta! in cljs, metadata needs to be changed in the compiler state."
  [target-ns & import-data]
  (doall
   (for [[src-sym _ target-name _] import-data]
     (adjust-var-meta! target-ns target-name src-sym)))
  nil)

(defmacro _import-var-data [import-data]
  (let [import-data-filtered #?(;; silently skip over macros for cljs JVM, they must be included by clj
                                :clj (filter (fn [[_ type _ _]] (not (= :macro type))) import-data)
                                ;; import all, including macros, for cljs JavaScript (aka bootstrapped cljs, aka self-hosted cljs)
                                :cljs import-data)
        import-cmds (map
                     (fn [[src-sym _type new-name new-meta]]
                       `(def ~(with-meta new-name (dissoc new-meta :name)) ~src-sym))
                     import-data-filtered)
        cmds (concat import-cmds [`(fixup-vars ~*ns* ~@import-data-filtered)])]
    `(do ~@cmds)) )


(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& raw-syms]
  (let [import-data (helper/syms->import-data raw-syms resolve-sym meta-from-resolved {})]
    `(_import-var-data ~import-data)))

(defmacro import-vars-with-mods
  "Imports a list of vars from other namespaces modifying imports via `opts`. "
  [opts & raw-syms]
  (let [import-data (helper/syms->import-data raw-syms resolve-sym meta-from-resolved opts)]
    `(_import-var-data ~import-data)))

;; --- potemkin.types

(defmacro defprotocol+
  "Currently a no-op for cljs."
  [name & body]
  `(defprotocol ~name ~@body))
