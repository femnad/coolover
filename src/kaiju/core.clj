(ns kaiju.core
  (:gen-class)
  (:require [clj-http.lite.client :as client])
  (:require [clojure.data.json :as json])
  (:require [maailma.core :as m])
  (:require [clojure.tools.cli :refer [parse-opts]]))

(def config-resource "config.edn")

(def api-suffix "rest/api/2")

(defn get-issue-field [issue field]
  (get-in issue ["fields" field]))

(defn get-issue-summary [issue]
  (format "%s: %s\n%s\n"
          (get issue "key")
          (get-issue-field issue "summary")
          (get-issue-field issue "description")))

(defn get-search-body [query max-results]
  (json/write-str {:jql query :maxResults max-results}))

(defn get-url [service-url endpoint]
  (format "%s/%s/%s" service-url api-suffix endpoint))

(defn request
  ([url body content-type basic-auth]
   (client/post url {:body body
                     :content-type content-type
                     :basic-auth basic-auth})
   ([url basic-auth]
    (client/get url {:basic-auth basic-auth}))))

(defn search-issues [config query]
  (let [search-url (get-url (get-in config [:service :url]) "search")
        user (get-in config [:credentials :user])
        password (get-in config [:credentials :password])]
    (let [raw-issues (client/post search-url
                                  {:basic-auth [user password]
                                   :body (get-search-body query 10)
                                   :content-type :json})]
    (get (json/read-str (:body raw-issues)) "issues"))))

(defn get-config []
  (m/build-config (m/resource config-resource)))

(defn get-project-query [project-name]
  (format "project = %s" project-name))

(defn get-all-projects [config]
  (let [search-url (get-url (get-in config [:service :url]) "project")]
    (let [projects
      (-> search-url
          client/get
          :body
          json/read-str)]
      (map #(format "%s - %s" (get % "name") (get % "key")) projects))))

(defn list-issues [project]
  (let [config (get-config)
        query (get-project-query project)
        issues (search-issues config query)]
    (doseq [issue issues]
      (println (get-issue-summary issue)))))

(def cli-options
  [["-p" "--project <project>" "project key"]])

(defn -main [& args]
  (let [parsed-args (parse-opts args cli-options)]
    (if (= (first (:arguments parsed-args)) "list-projects")
      (let [projects (get-all-projects (get-config))]
        (doseq [project projects]
          (println project)))
      (if-not (nil? (get-in parsed-args [:options :project]))
        (list-issues (get-in parsed-args [:options :project]))
        (println "usage: kaiju [list-projects] [-p <project-name]")))))
