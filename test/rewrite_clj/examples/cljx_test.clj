(ns rewrite-clj.examples.cljx-test
  (:require [midje.sweet :refer :all]
            [rewrite-clj.zip :as z]))

;; ## Reader Macro Detection

(defn- cljx-macro?
  "Check if the given zipper node contains a cljx reader macro ('#+...' or '#-...')."
  [loc]
  (when (= (z/tag loc) :reader-macro)
    (when-let [macro-sym (z/down loc)]
      (when (= (z/tag macro-sym) :token)
        (let [sym (z/sexpr macro-sym)]
          (when (symbol? sym)
            (let [^String nm (name sym)]
              (or (.startsWith nm "+")
                  (.startsWith nm "-")))))))))

(tabular
  (fact "about cljx macro detection."
        (let [loc (z/of-string ?data)]
          (cljx-macro? loc) => ?pred))
  ?data                 ?pred
  "#+clj 123"           truthy
  "#-clj 123"           truthy
  "#clj  123"           falsey
  "123"                 falsey)

;; ## Replace Form w/ Spaces

(defn- replace-with-spaces
  "Replace the given reader macro node with spaces. The resulting zipper
   will be on the first element following it."
  [zloc]
  (let [w (z/length zloc)]
    (-> zloc
        (z/prepend-space w)        ;; add space
        z/remove*                  ;; remove original (without removing an extra space)
        z/next)))                  ;; go to following node

(fact "about replacing a form with spaces."
      (let [loc (z/of-string "(hello [\"world\" \"you\"] 2)")
            rloc (-> loc z/next z/next z/next replace-with-spaces)]
        (z/sexpr rloc)       => "you"
        (z/root-string rloc) => "(hello [        \"you\"] 2)"))

;; ## Remove Reader Macro

(defn- remove-reader-macro-symbol
  "Remove the macro symbol part of the given reader macro node."
  [zloc]
  (-> zloc
      z/splice                   ;; remove the macro wrapper
      replace-with-spaces        ;; replace the '+...'/'-...' part with spaces
      z/prepend-space))          ;; insert a space to make up for the missing '#'

(fact "about removing the symbol part of a reader macro."
      (let [loc (z/of-string "(+ 0 #+clj 123 4)")
            rloc (-> loc z/next z/next z/next remove-reader-macro-symbol)]
        (z/sexpr rloc) => 123
        (z/root-string rloc) => "(+ 0       123 4)"))

;; ## cljx step

(defn- handle-reader-macro
  "Handle a reader macro node by either removing it completely or only the macro part."
  [active-profiles zloc]
  {:pre (= (z/tag zloc) :reader-macro)}
  (let [profile (-> zloc z/down z/sexpr name)
        active? (contains? active-profiles (subs profile 1))
        print? (.startsWith profile "+")]
    (if (or (and active? (not print?))
            (and (not active?) print?))
      (replace-with-spaces zloc)
      (remove-reader-macro-symbol zloc))))

(tabular
  (fact "about reader macro handling."
        (let [loc (z/of-string ?data)
              rloc (handle-reader-macro #{"clj"} loc)]
          (z/root-string rloc) => ?result))
  ?data              ?result
  "#+clj 123"        "      123"
  "#-clj 123"        "         "
  "#+clx 123"        "         "
  "#-clx 123"        "      123")

;; ## cljx

(defn cljx-walk
  "Replace all occurences of profile reader macros."
  [root active-profiles]
  (z/prewalk
    root
    cljx-macro?
    #(handle-reader-macro active-profiles %)))

(defn cljx-string
  "Run cljx on the given string."
  [data profiles]
  (let [active-profiles (set (map name profiles))
        zloc (z/of-string data)]
    (z/root-string
      (cljx-walk zloc active-profiles))))

      (let [data (str "(defn debug-inc\n"
                      "  [x]\n"
                      "  #+debug (println #-compact :debug 'inc x)\n"
                      "  (inc x))")]
        (tabular
          (fact "about cljx."
                (cljx-string data ?profiles) => ?result)
          ?profiles, ?result
          #{}
          (str "(defn debug-inc\n"
               "  [x]\n"
               "                                           \n"
               "  (inc x))")

          #{:debug}
          (str "(defn debug-inc\n"
               "  [x]\n"
               "          (println           :debug 'inc x)\n"
               "  (inc x))")

          #{:debug :compact}
          (str "(defn debug-inc\n"
               "  [x]\n"
               "          (println                  'inc x)\n"
               "  (inc x))")))
