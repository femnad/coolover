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

(defn- get-necessary-tokens [fields-in-use]
  (filter #(contains? fields-in-use (first %)) tokens))

(defn get-style-format-str [a-map]
  (->> a-map
      keys
      set
      get-necessary-tokens
      (map #(last %) ,)
      (apply str ,)))
