(ns coolover.core
  (:gen-class)
  (:require [clj-http.lite.client :as client])
  (:require [clojure.data.json :as json])
  (:require [maailma.core :as m])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clansi :refer [style]]))


(def api-suffix "rest/api/2")

(def config-resource "config.edn")

(def default-content-type :json)

(def exec-name "coolover")

(defn get-config []
  (m/build-config (m/resource config-resource)))

(defn get-issue-field [issue field]
  (get-in issue ["fields" field]))

(defn get-browse-url [issue-key]
  (format "%s/browse/%s" (get-in (get-config) [:service :url]) issue-key))

(defn get-issue [issue]
  (let [get-field #(get-issue-field issue %)
        issue-key (get issue "key")]
    {:title issue-key
     :summary (get-field "summary")
     :description (get-field "description")
     :browse-url (get-browse-url issue-key)}))

(defn format-issue [issue]
  (let [issue-map (get-issue issue)]
    (format "%s: %s [%s]\n\n%s\n"
            (style (:title issue-map) :green :underline)
            (style (:summary issue-map) :yellow :underline)
            (style (:browse-url issue-map) :magenta)
            (:description issue-map))))

(defn get-search-body [query max-results]
  (json/write-str {:jql query :maxResults max-results}))

(defn get-url [endpoint]
  (let [service-url (get-in (get-config) [:service :url])]
    (format "%s/%s/%s" service-url api-suffix endpoint)))

(defn get-basic-auth-header []
  {:auth (map #(get-in (get-config) [:credentials %]) [:user :password])})

(defn- has-auth []
  (contains? (get-config) :credentials))

(defn- build-args-with-headers [url headers]
  (if (has-auth)
    (list url (merge headers (get-basic-auth-header)))
    (list url headers)))

(defn- build-args [url]
  (if (has-auth)
    (list url (get-basic-auth-header))
    (list url)))

(defn request
  ([url body]
   (apply client/post
         (build-args-with-headers url
                      {:body body
                       :content-type default-content-type})))
   ([url]
    (apply client/get (build-args url))))

(defn- get-resource [resource resource-id]
  (let [resource-url (format "%s/%s" (get-url "issue") resource-id)]
    (->
     resource-url
     request
     :body
     json/read-str)))

(defn show-issue [issue-key]
  (println (format-issue (get-resource "issue" issue-key))))

(defn search-issues [config query max-results]
  (let [search-url (get-url "search")
        basic-auth (map #(get-in config [:credentials %]) [:user :password])
        search-body (get-search-body query max-results)]
    (->
     search-url
     (request search-body)
     :body
     json/read-str
     (get "issues"))))

(defn get-query [query-map order-by]
  (str (clojure.string/join
      " and " (map
               #(format "%s = %s"
                        (first %)
                        (second %))
               (into-array query-map)))
     " order by " order-by))

(defn format-projects []
  (let [search-url (get-url "project")]
    (let [projects
      (-> search-url
          request
          :body
          json/read-str)]
      (map #(format "%s - %s" (get % "name") (get % "key")) projects))))

(defn list-projects []
  (doseq [project (format-projects)]
    (println project)))

(defn list-issues [project order-by max-results]
  (let [config (get-config)
        query (get-query {"project" project} order-by)
        issues (search-issues config query max-results)]
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
  ((keyword mode-name) modes))

(defn- get-mode-from-parsed-args [parsed-args]
  (->
   parsed-args
   :arguments
   first
   get-mode))

(defn- parse-args [args]
  (parse-opts args cli-options))

(defn get-usage [args]
  (format "usage: %s [options] [list-projects|list-issues|show-issue]\n%s"
          exec-name
          (:summary (parse-args args))))

(defn- get-mode-from-args [parsed-args]
  (get-mode-from-parsed-args parsed-args))

(defn- get-selected-mode [args]
  (let [parsed-args (parse-args args)
        mode (get-mode-from-parsed-args parsed-args)
        mode-fn (:fn mode)
        formal-args (:args mode)
        actual-args (map #(% (:options parsed-args)) formal-args)]
    (list mode-fn actual-args)))

(defn- nil-in-args? [mode]
  (not (not-any? nil? (last mode))))

(defn run-mode [mode]
  (apply apply mode))

(defn -main [& args]
  (let [mode (get-selected-mode args)]
    (if (nil-in-args? mode)
      (println (get-usage args))
      (run-mode mode))))
