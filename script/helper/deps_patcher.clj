(ns helper.deps-patcher
  "Quick and dirty little project.clj and deps.edn patcher"
  (:require [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]))

(defn- find-project-deps
  "Interested in top level deps only at this time."
  [zloc]
  (-> zloc
      z/down
      (z/find-value z/right :dependencies)
      z/next))

(defn- remove-project-deps [zloc dep-syms]
  (z/subedit->> (find-project-deps zloc)
                z/down
                (iterate (fn [zloc]
                           (when (not (z/end? zloc))
                             (if (dep-syms (-> zloc z/down z/sexpr))
                               (-> zloc z/remove z/next)
                               (-> zloc z/right)))))
                (take-while identity)
                last))

(defn- add-project-deps [zloc new-deps]
  (let [zloc-deps (find-project-deps zloc)
        indent (n/spaces (-> zloc-deps z/node meta :col))]
    (reduce (fn [zloc dep]
              (-> zloc
                  (z/append-child* (n/newlines 1))
                  (z/append-child* indent)
                  (z/append-child dep)))
            zloc-deps
            new-deps)))

(defn update-project-deps [{:keys [filename additions removals] :as kwargs}]
  (println kwargs)
  (-> filename
      z/of-file
      (remove-project-deps removals)
      z/root
      z/edn*
      z/next
      (add-project-deps additions)
      z/root-string
      (->> (spit filename))))

(defn- remove-deps-deps [zloc dep-syms]
  (reduce (fn [zloc sym]
            (if (z/get zloc sym)
              (z/subedit-> zloc (z/get sym) z/remove z/remove)
              zloc))
          (z/get zloc :deps)
          dep-syms))

(defn- add-deps-deps [zloc new-deps]
  (let [zloc-deps (z/get zloc :deps)
        indent (n/spaces (-> zloc-deps z/node meta :col))]
    (reduce (fn [zloc [k v]]
              (-> zloc
                  (z/append-child* (n/newlines 1))
                  (z/append-child* indent)
                  (z/append-child k)
                  (z/append-child v)))
            zloc-deps
            new-deps)))

(defn update-deps-deps [{:keys [filename additions removals] :as kwargs}]
  (println kwargs)
  (-> filename
      z/of-file
      (remove-deps-deps removals)
      z/root
      z/edn*
      z/next
      (add-deps-deps additions)
      z/root-string
      (->> (spit filename))))
