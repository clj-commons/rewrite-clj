(ns rewrite-clj.zip.test-helper
  "It can be tricky and error prone to navigate to a desired location in a zipper.
  These helpers allow us to mark our desired location with a special ⊚ character
  and spit out that same character to reflect the location."
  (:require [clojure.string :as str]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]))

(defn pos-and-s
  "Given s that includes a single loc prefix ⊚, return map with
  - `:s` s without loc prefix
  - `:pos`
    - `:row` row of prefix
    - `:col` col or prefix"
  [s]
  (let [[row col] (-> s
                      (str/replace #"(?s)⊚.*" "")
                      (str/split #"\n" -1)
                      ((juxt count #(-> % last count inc))))]
    {:s (str/replace s "⊚" "") :pos {:row row :col col}}))


(defn of-locmarked-string
  "Return zloc for string `s` located at node prefixed with ⊚ marker.
  Use ◬ as first char to locate to root forms node."
  [s opts]
  (if (str/starts-with? s "◬")
    (z/of-string* (subs s 1) opts)
    (let [{:keys [pos s]} (pos-and-s s)
          {target-row :row target-col :col} pos
          zloc (z/of-string* s opts)]
      (loop [zloc (z/down* zloc)]
        (let [{:keys [row col]} (meta (z/node zloc))]
          (cond
            (and (= target-row row) (= target-col col))
            zloc

            (z/end? zloc)
            (throw (ex-info (str "Oops, of-locmarked-string failed to locate to ⊚ marked found at [row col]:" [target-row target-col]) {}))

            :else
            (recur (z/next* zloc))))))))

(defn- row-num [zloc]
  (loop [zloc (z/prev* zloc)
         rows 0]
    (if (not zloc)
      (inc rows)
      (if (or (z/linebreak? zloc) (n/comment? (z/node zloc)))
        (recur (z/prev* zloc) (inc rows))
        (recur (z/prev* zloc) rows)))))

(defn- col-num [zloc]
  (loop [zloc zloc
         cols 0]
    (let [up (z/up* zloc)
          left (z/left* zloc)]
      (cond
        (and left (or (z/linebreak? left) (-> left z/node n/comment?)))
        (inc cols)

        left
        (recur left (+ cols (z/length left)))

        up
        (recur up (+ cols (-> up z/node n/leader-length)))

        :else
        (inc cols)))))

(defn root-locmarked-string
  "Return root string for `zloc` with current node prefixed with ⊚ marker,
  if located at root forms node string will start with ◬"
  [zloc]
  (if (= :forms (z/tag zloc))
    (str "◬" (z/root-string zloc) )
    (let [row (row-num zloc)
          col (col-num zloc)
          s (z/root-string zloc)
          lines (str/split s #"\n" -1)
          line (nth lines (dec row))]
      (str/join "\n"
                (concat
                  (subvec lines 0 (dec row))
                  [(str (subs line 0 (dec col))
                        "⊚"
                        (subs line (dec col)))]
                  (subvec lines row))))))
