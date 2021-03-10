(ns helper.status
  (:require [clojure.string :as string]))

(def format-to-column 80)

(def ansi-codes {:fg-black "30"
                 :fg-red "31"
                 :fg-green "32"
                 :fg-yellow "33"
                 :fg-blue "34"
                 :fg-white "37"

                 :bg-red "41"
                 :bg-strong-red "101"
                 :bg-green "42"
                 :bg-strong-green "102"
                 :bg-yellow "43"
                 :bg-strong-yellow "103"
                 :bg-cyan "46"

                 :reset "0"})

(defn- ansi-esc* [ & codes ]
  (str "\u001b[" (string/join ";" codes) "m"))

(defn- ansi-esc [ & lookups ]
  (apply ansi-esc* (map #(get ansi-codes %) lookups)))

(defn- visible-length [ s ]
  (-> s
      string/split-lines
      last
      (string/replace  #"\u001b\[[0-9;]+m" "")
      count))

(defn- colorize [codes s]
  (str codes s (ansi-esc :reset)))

(defn- str-repeat [n s]
  (apply str (repeat n s)))

(defn- pad-right [s to-len c]
  (str s (str-repeat (- (inc to-len) (visible-length s))
                     c)))

(defn- left-pad-lines [ lines padding ]
  (map #(str padding %) lines))

(defn- right-pad-lines [ lines max-len]
  (map #(pad-right % max-len " ") lines))

(defn- block-lines [lines color max-len]
  (let [blocked (-> lines
                    (left-pad-lines " ")
                    (right-pad-lines max-len))]
    (map #(colorize color %) blocked)) )

(defn- block-problem-lines [lines color]
  (block-lines lines color (dec format-to-column)))

(defn- wrap-problem-block [lines prefix suffix color]
  (concat [(colorize color (pad-right prefix format-to-column "-"))]
          (map #(str (colorize color " ") % ) lines)
          [(colorize color (str " " (str-repeat (- format-to-column (count suffix)) "-")
                                suffix))]))

(defn- format-problem-line [msg {:keys [prefix suffix color-bookends color-msg]}]
  (let [lines (-> msg
                  string/split-lines
                  (block-problem-lines color-msg)
                  (wrap-problem-block prefix suffix color-bookends))]
    (str "\n" (string/join "\n" lines))))

(defn- block-info-lines [lines color]
  (let [max-len (->> lines
                     (map count)
                     (filter #(<= % format-to-column))
                     (reduce max 0)
                     inc)]
    (block-lines lines color max-len)))

(defn- bookend-info-lines [lines prefix suffix color]
  (let [indent (str-repeat (visible-length prefix) " ")
        indented (->> (rest lines)
                      (map #(str indent %))
                      (into [(str (colorize color prefix) (first lines))]))]
    (concat (butlast indented)
            [(str (last indented) (colorize color suffix))])))

(defn- add-info-dash-break
  "Add an hidden-ish dash break that will help bring focus when pasting to plain ASCII"
  [lines]
  (let [last-line (last lines)]
    (concat (butlast lines)
            [(str last-line
                  (colorize (ansi-esc :fg-black :bg-black)
                            (str-repeat (- (inc format-to-column) (visible-length last-line))
                                        "-")))])))

(defn- format-info-line [msg {:keys [prefix suffix color-bookends color-msg]}]
  (let [lines (-> msg
                  string/split-lines
                  (block-info-lines color-msg)
                  (bookend-info-lines prefix suffix color-bookends)
                  (add-info-dash-break))]
    (str "\n" (string/join "\n" lines))))

(defn line [type msg]
  (println (case type
             :detail msg
             :info (format-info-line msg {:prefix "["
                                          :suffix "]"
                                          :color-bookends (ansi-esc :bg-green :fg-green)
                                          :color-msg (ansi-esc :bg-cyan :fg-black)})
             :warn (format-problem-line msg {:prefix "-WARNING"
                                             :color-bookends (ansi-esc :bg-yellow :fg-black)
                                             :color-msg (ansi-esc :bg-strong-yellow :fg-black)})
             :error (format-problem-line msg {:prefix "-ERROR:"
                                              :suffix "-"
                                              :color-bookends (ansi-esc :bg-red :fg-black)
                                              :color-msg (ansi-esc :bg-yellow :fg-black)})
             (throw (ex-info (format "scripting error: unrecognized type: %s for status msg: %s" type msg) {})))))

(defn fatal
  ([msg] (fatal msg 1))
  ([msg exit-code]
   (line :error msg)
   (System/exit exit-code)))
