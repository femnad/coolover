(ns kaiju.core
  (:gen-class)
  (:require [clj-http.lite.client :as client])
  (:require [clojure.data.json :as json])
  (:require [maailma.core :as m]))

(def config-resource "config.edn")

(def api-suffix "rest/api/2")

(defn get-issue-field [issue field]
  (get-in issue ["fields" field]))

(defn get-issue-summary [issue]
  (format "%s: %s - %s"
          (get issue "key")
          (get-issue-field issue "summary")
          (get-issue-field issue "description")))

(defn get-search-body [query max-results]
  (json/write-str {:jql query :maxResults max-results}))

(defn get-url [service-url endpoint]
  (format "%s/%s/%s" service-url api-suffix endpoint))

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

(defn list-issues [project]
  (let [config (get-config)
        query (get-project-query project)
        issues (search-issues config query)]
    (for [issue issues]
      (println (get-issue-summary issue)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (= (count args) 1)
    (list-issues (first args))
    (println "usage: kaiju <project-name>")))
