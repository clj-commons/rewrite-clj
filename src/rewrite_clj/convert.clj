(ns ^{ :doc "Convert EDN tree to Clojure data."
       :author "Yannick Scherer"}
  rewrite-clj.convert
  (:require [clojure.walk :as w]))

;; # Tree -> S-Expression

;; ## Base

(defmulti ->sexpr 
  (fn [v] (when (vector? v) (first v))) 
  :default nil)

(defn- children->sexprs
  [v]
  (->> (rest v)
    (filter (comp not #{:comment :whitespace :newline} first))
    (map ->sexpr)))

(defn- wrap-sexpr
  [kw v]
  (list (symbol kw) (->sexpr (second v))))

;; ## Simple Conversions

(defmethod ->sexpr nil [v]
  (if (not (vector? v)) 
    (throw (Exception. (str "Not a vector: " v)))
    (throw (Exception. (str "Cannot convert type '" (first v) "' to s-expression.")))))

(defmethod ->sexpr :token [v] (second v))
(defmethod ->sexpr :eval [v] (->sexpr (second v)))
(defmethod ->sexpr :list [v] (apply list (children->sexprs v)))
(defmethod ->sexpr :vector [v] (vec (children->sexprs v)))
(defmethod ->sexpr :set [v] (set (children->sexprs v)))
(defmethod ->sexpr :map [v] (apply hash-map (children->sexprs v)))
(defmethod ->sexpr :multi-line [v]
  (let [parts (rest v)]
    (apply str (butlast (interleave parts (repeat "\n"))))))

;; ## Wrapped

(defmethod ->sexpr :deref [v] (wrap-sexpr 'deref v))
(defmethod ->sexpr :var [v] (wrap-sexpr 'var v))
(defmethod ->sexpr :quote [v] (wrap-sexpr 'quote v))
(defmethod ->sexpr :syntax-quote [v] (wrap-sexpr 'quote v))
(defmethod ->sexpr :unquote [v] (wrap-sexpr 'unquote v))
(defmethod ->sexpr :unquote-splicing [v] (wrap-sexpr 'unquote-splicing v))

;; ## Metadata

(defn- wrap-meta
  [v]
  (let [[m v] (children->sexprs v)
        m (cond (symbol? m) { :tag m } 
                (keyword? m) { m true }
                :else m)]
    (vary-meta v merge m)))

(defmethod ->sexpr :meta [v] (wrap-meta v))
(defmethod ->sexpr :meta* [v] (wrap-meta v))

;; ## Functions
;;
;; TODO: Make more functional by removing the atom that counts the `%...` variables
;;       to substitute.

(defn- replace-fn-syms
  [syms counter body]
  (letfn [(nth! [v] 
            (swap! counter
                   (fn [[o b]]
                     (if (= v 4) 
                       [o true]
                       [(max o v) b]))) 
            (nth syms (dec v)))]
    (w/postwalk
      (fn [x]
        (if-not (symbol? x) 
          x
          (condp = (str x)
            "%"  (nth! 1)
            "%1" (nth! 1)
            "%2" (nth! 2)
            "%3" (nth! 3)
            "%&" (nth! 4)
            x)))
      body)))

(defmethod ->sexpr :fn
  [v]
  (let [body (children->sexprs v)
        syms (repeatedly #(gensym))
        cnt (atom [0 nil])
        body (replace-fn-syms syms cnt body)]
    (list 'fn 
          (vec 
            (concat
              (take (first @cnt) syms)
              (when (second @cnt) ['& (nth syms 3)]))) 
          body)))

;; # S-Expression -> Tree


;; ## Base

(defmulti ->tree
 (fn [v]
   (cond (map? v) :map
         (vector? v) :vector
         (set? v) :set
         (seq? v) :list
         (list? v) :list
         :else (class v)))
  :default :token)

(def ^:private ^:const SPACE [:whitespace " "])

(defn- seq->tree
  [tag sq]
  (vec (list* tag (butlast (mapcat vector (map ->tree sq) (repeat SPACE))))))

;; ## Simple Types

(defmethod ->tree :token [v] [:token v])
(defmethod ->tree :list [v] (seq->tree :list v))
(defmethod ->tree :vector [v] (seq->tree :vector v))
(defmethod ->tree :set [v] (seq->tree :set v))
(defmethod ->tree :map [v] (seq->tree :map (apply concat (seq v))))
(defmethod ->tree clojure.lang.Var [v] [:var (->tree (:name (meta v)))])
