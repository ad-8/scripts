(ns scratchpad
  (:require  [babashka.http-client :as http]
             [cheshire.core :as json]
             [clojure.string :as str]
             [clojure.java.shell :refer [sh]]
             [clojure.walk]))

; ready to go
(defn happy-get [url]
  (->> (http/get url)
       :body
       json/decode
       clojure.walk/keywordize-keys))


(def now (java.time.ZonedDateTime/now))
now

(defn babashka-latest-version []
  (-> (sh "curl" "https://api.github.com/repos/babashka/babashka/tags")
      :out
      (json/parse-string true)
      first
      :name))

(-> (sh "curl" "https://api.github.com/repos/babashka/babashka/tags")
    
    :out
    (json/parse-string true) 
    :name)

(babashka-latest-version)


(sh "df" "-h" "/" "--output=avail")