(ns coolover.core
  (:gen-class)
  (:require [clj-http.lite.client :as client])
  (:require [clojure.data.json :as json])
  (:require [maailma.core :as m])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clansi :refer [style]])
  (:require [clj-time.format :as f])
  (:require [clojure.string :as s])
  (:use [clojure.java.shell :only [sh]]))

(def api-suffix "rest/api/2")

(def config-resource "config.edn")

(def default-content-type :json)

(def exec-name "coolover")

(defn get-config []
  (m/build-config (m/resource config-resource)))

(defn- eval-command-to-get-password [command]
                 (-> command
                     (s/split , #" ")
                     (#(apply clojure.java.shell/sh %))
                     :out
                     (s/split , #"\n")
                     first))

(defn get-issue-field [issue field]
  (get-in issue ["fields" field]))

(defn get-browse-url [issue-key]
  (format "%s/browse/%s" (get-in (get-config) [:service :url]) issue-key))

(defn- format-date [a-date]
  (f/unparse (f/formatters :mysql) (f/parse (f/formatters :date-time) a-date)))

(defn get-issue-map [issue]
  (let [get-field #(get-issue-field issue %)
        issue-key (get issue "key")]
    {:title issue-key
     :summary (get-field "summary")
     :description (get-field "description")
     :created (format-date (get-field "created"))
     :updated (format-date (get-field "updated"))
     :browse-url (get-browse-url issue-key)}))

(defn- style-items [container item-style-pairs]
  (map #(style ((first %) container) (second %)) item-style-pairs))

(defn- style-issue [issue-map]
  (style-items issue-map
               {:title :green
                :summary :yellow
                :created :cyan
                :updated :cyan
                :browse-url :magenta
                :description :normal}))

(defn format-issue [issue]
  (let [issue-map (get-issue-map issue)
        styled-issue (style-issue issue-map)]
    (apply format "%s: %s <%s - %s>\n[%s]\n%s\n" styled-issue)))

(defn get-search-body [query max-results]
  (json/write-str {:jql query :maxResults max-results}))

(defn get-url [config endpoint]
  (let [service-url (get-in config [:service :url])]
    (format "%s/%s/%s" service-url api-suffix endpoint)))

(defn- get-auth-pair-from-config [config]
  (let [creds (:credentials config)]
    (if (contains? creds :password)
      (map #(% creds) [:user :password])
      (list (:user creds)
            (eval-command-to-get-password (:password-eval creds))))))

(defn get-basic-auth-header [config]
  {:basic-auth (get-auth-pair-from-config config)})

(defn- has-auth [config]
  (contains? config :credentials))

(defn- build-args-with-headers [url headers config]
  (if (has-auth config)
    (list url (merge headers (get-basic-auth-header config)))
    (list url headers)))

(defn- build-args [url config]
  (if (has-auth config)
    (list url (get-basic-auth-header config))
    (list url)))

(defn request
  ([url body config]
   (apply client/post
         (build-args-with-headers url
                      {:body body
                       :content-type default-content-type}
                      config)))
   ([url config]
    (apply client/get (build-args url config))))

(defn- get-resource [resource resource-id config]
  (let [resource-endpoint
        (->> resource
            name
            (get-url config ,))
        resource-url (format "%s/%s" resource-endpoint resource-id)]
    (-> resource-url
        (request config)
        :body
        json/read-str)))

(defn- get-issue [issue-id config]
  (get-resource "issue" issue-id config))

(defn show-issue [config issue-key]
  (println (format-issue (get-issue issue-key config))))

(defn- get-issue-attachments [issue]
  (->> (get-in issue ["fields" "attachment"])
       (map #(get % "content"))))

(defn- download-attachment [attachment-link]
  (:body (request attachment-link)))

(defn- download-issue-attachment [issue-id]
  (let [issue-attachments
        (-> issue-id
            get-issue
            get-issue-attachments)]
    (map download-attachment issue-attachments)))

(defn basename [url]
  (apply str (drop (inc (s/last-index-of url "/")) url)))

(defn- save-attachment [link path-prefix]
  (spit (format "%s/%s" path-prefix (basename link))
        (download-attachment link)))

(defn download-attachments-of-issues [issues path-prefix]
  (let [attachment-links (->> issues
                              (map #(get % "key"))
                              (map #(get-issue %))
                              (map #(get-issue-attachments %))
                              (filter #(not (empty? %)))
                              flatten)]
    (doseq [link attachment-links]
      (save-attachment link path-prefix))))

(defn search-issues [config query max-results]
  (let [search-url (get-url config "search")
        basic-auth (map #(get-in config [:credentials %]) [:user :password])
        search-body (get-search-body query max-results)]
    (->
     search-url
     (request , search-body config)
     :body
     json/read-str
     (get "issues"))))

(defn get-query [query-map order-by]
  (str (s/join
      " and " (map
               #(format "%s = %s"
                        (first %)
                        (second %))
               (into-array query-map)))
     " order by " order-by))

(defn format-projects [config]
  (let [search-url (get-url config "project")]
    (let [projects
      (-> search-url
          (request config)
          :body
          json/read-str)]
      (map #(format "%s - %s" (get % "name") (get % "key")) projects))))

(defn list-projects [config]
  (doseq [project (format-projects config)]
    (println project)))

(defn- get-issues [config project order-by max-results]
  (let [query (get-query {"project" project} order-by)]
        (search-issues config query max-results)))

(defn list-issues [config project order-by max-results]
  (let [issues (get-issues config project order-by max-results)]
    (doseq [issue issues]
      (println (format-issue issue)))))

(def cli-options
  [["-p" "--project <project>" "project key"]
   ["-o" "--order-by <field>" "order by field"
    :default "created"]
   ["-m" "--max-results <number>" "maximum number of results"
    :default 10]
   ["-i" "--issue-key <issue-key>" "issue key"]])

(def modes
  {:list-projects {:fn list-projects :args []}
   :list-issues {:fn list-issues :args [:project :order-by :max-results]}
   :show-issue {:fn show-issue :args [:issue-key]}})

(defn get-mode [mode-name]
  (-> mode-name
      keyword
      (, modes)))

(defn- get-mode-from-parsed-args [parsed-args]
  (->
   parsed-args
   :arguments
   first
   get-mode))

(defn- parse-args [args]
  (parse-opts args cli-options))

(defn get-usage [parsed-args]
  (let [error-message (s/join "" (:errors parsed-args))]
    (format "usage: %s <modes> <options>\n\nModes:\n%s\n\nOptions:\n%s\n%s"
            exec-name
            (s/join " | " (map name (keys modes)))
            (:summary parsed-args)
            error-message)))

(defn- get-selected-mode [parsed-args]
  (let [mode (get-mode-from-parsed-args parsed-args)
        mode-fn (:fn mode)
        formal-args (:args mode)
        actual-args (map #(% (:options parsed-args)) formal-args)]
    (list mode-fn actual-args)))

(defn- invalid-mode? [mode]
  (some nil? mode))

(defn run-mode [[mode args] config]
  (apply mode (cons config args)))

(defn -main [& args]
  (let [parsed-args (parse-args args)
        config (get-config)]
    (if (or (= (count args) 0)
            (:errors parsed-args))
      (println (get-usage parsed-args))
      (let [mode (get-selected-mode parsed-args)]
        (if (invalid-mode? mode)
          (println (get-usage parsed-args))
          (run-mode mode config))))))
