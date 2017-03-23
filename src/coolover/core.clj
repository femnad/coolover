(ns coolover.core
  (:gen-class)
  (:require [clj-http.lite.client :as client])
  (:require [clojure.data.json :as json])
  (:require [maailma.core :as m])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clansi :refer [style]]))

(def config-resource "config.edn")

(def api-suffix "rest/api/2")

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

(defn get-url [service-url endpoint]
  (format "%s/%s/%s" service-url api-suffix endpoint))

(defn request
  ([url body content-type basic-auth]
   (client/post url {:body body
                     :content-type content-type
                     :basic-auth basic-auth}))
   ([url basic-auth]
    (client/get url {:basic-auth basic-auth})))

(defn search-issues [config query max-results]
  (let [search-url (get-url (get-in config [:service :url]) "search")
        basic-auth (map #(get-in config [:credentials %]) [:user :password])
        search-body (get-search-body query max-results)]
    (->
     search-url
     (request search-body :json basic-auth)
     :body
     json/read-str
     (get "issues"))))

(defn get-basic-auth-pair [config]
  (map #(get-in config [:credentials %]) [:user :password]))

(defn get-query [query-map order-by]
  (str (clojure.string/join
      " and " (map
               #(format "%s = %s"
                        (first %)
                        (second %))
               (into-array query-map)))
     " order by " order-by))

(defn get-all-projects [config]
  (let [search-url (get-url (get-in config [:service :url]) "project")]
    (let [projects
      (-> search-url
          (request (get-basic-auth-pair config))
          :body
          json/read-str)]
      (map #(format "%s - %s" (get % "name") (get % "key")) projects))))

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
   ["-n" "--number-of-results <number>" "maximum number of results"
    :default 10]])

(defn get-usage [parsed-args]
  (format "usage: %s [options] [list-projects]\n%s" exec-name (:summary parsed-args)))

(defn -main [& args]
  (let [parsed-args (parse-opts args cli-options)
        get-option #(get-in parsed-args [:options %])]
    (if (= (first (:arguments parsed-args)) "list-projects")
      (let [projects (get-all-projects (get-config))]
        (doseq [project projects]
          (println project)))
      (if-not (nil? (get-option :project))
        (list-issues (get-option :project)
                     (get-option :order-by)
                     (get-option :number-of-results))
        (println (get-usage parsed-args))))))
