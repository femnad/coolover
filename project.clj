(defproject coolover "0.0.1"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http-lite "0.3.0"]
                 [metosin/maailma "0.2.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main ^:skip-aot coolover.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
