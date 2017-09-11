(ns coolover.format-issue
  (:gen-class))

(def style-format-str "%s: %s <%s - %s>\n[%s]\n%s\n")

(def tokens
  [[:title "%s: "]
   [:summary "%s "]
   [:created "<%s"]
   [:updated " - %s"]
   [:created ">\n"]
   [:browse-url "[%s]\n"]
   [:description "%s\n"]])

(defn get-style-format-str [a-map]
  (loop [remaining tokens
         acc-seq []]
    (if (empty? remaining)
      (apply str acc-seq)
      (let [cur-pair (first remaining)
            key-str (first cur-pair)
            fmt-str (last cur-pair)]
        (if (contains? a-map key-str)
          (recur (rest remaining)
                 (conj acc-seq fmt-str))
          (recur (rest remaining) acc-seq))))))
