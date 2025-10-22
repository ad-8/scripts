#!/usr/bin/env bb
(ns waybar
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]
            [clojure.java.io :as io]
            [babashka.fs :as fs]
            [babashka.cli :as cli]
            [clojure.test :refer :all]
            [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.walk :refer [keywordize-keys]]))

(defn default []
  (println "default case"))

(defn waybar-date []
  (let [date-str (-> (shell {:out :string} "date '+%a %d.%m.'")
            :out
            str/trim)]
    (println date-str)))


(comment

  (-> (shell {:out :string} "date '+%a %d.%m.'")
      :out
      str/trim)

  ;;
  )


(let [action (first *command-line-args*)]
  (case action
    "date" (waybar-date)
    (default)))