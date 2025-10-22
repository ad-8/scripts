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


(defn- print-free-space [output-fmt]
  (let [free-space-on-root (->> (shell {:out :string} "df -h / --output=avail")
                                :out
                                str/split-lines
                                last
                                str/trim)
        fmt (format "󰋊 %s" free-space-on-root)
        free-space-int (Integer/parseInt (re-find #"\d+" free-space-on-root))
        class (if (< free-space-int 25) :low :ok)
        json (json/encode {:text fmt :class class})]

    (if (= "json" output-fmt)
      (println json)
      (println fmt))))

(defn waybar-disk []
  (print-free-space "json"))


(defn waybar-licht []
  (let [licht-val (slurp "/tmp/licht-curr-val")]
    (printf " %s" licht-val)))

(defn waybar-load []
  (let [loadavg (-> (shell {:out :string} "sh -c 'cat /proc/loadavg'")
                    :out
                    str/trim)
        load-one-min (-> loadavg (str/split #"\s") first)]
    (printf " %s" load-one-min)))



; total memory [GiB] as reported by `free -h`
(def host->memory {:ax-mac  7.7
                   :ax-fuji 15.0
                   :ax-bee 27.0})

(defn- get-hostname []
  (let [output (:out (shell {:out :string} "hostnamectl"))
        match (re-find #"Static hostname:\s*(\S+)" output)]
    (if match
      (last match)
      "unknown-hostname")))

(defn- determine-css-class
  "Determine the css class for waybar."
  [hostname used]
  (let [host-mem (get host->memory (keyword hostname))]
    (if (nil? host-mem)
      (if (> used 5.7) :low :ok)
      (if (> used (- host-mem 2.0)) :low :ok))))

;; TODO filter in clj like in rust
(defn- print-memory-info [output-fmt]
  (let [cmd "free -h | awk '/^Mem/ { print $3 \"/\" $2 }' | sed 's/i//g' | sed 's/G//'"
        mem (-> (shell {:out :string} "/usr/bin/env bash -c" cmd)
                :out
                str/trim
                (str/replace "," "."))
        [used _total] (str/split mem #"/")
        class (determine-css-class (get-hostname) (Float/parseFloat used))
        fmt (format "󰍛 %s" mem)
        json (json/encode {:text fmt :class class})]
    (if (= "json" output-fmt)
      (println json)
      (println fmt))))

(defn waybar-memory []
  (print-memory-info "json"))





(defn waybar-music []
  (printf "TODO music"))

(defn waybar-toggle []
  (printf "TODO toggle"))


; match e.g.: ["ProtonVPN DE#316" "DE#316"]
(defn waybar-vpn []
  (let [match (->> (shell {:out :string} "nmcli con show --active")
                   :out
                   (re-find #"ProtonVPN (\w+#\d+)|([A-Z]{2}-\d+)|muc")
                   (filter some?))
        out-str (if (and (some? match) (seq match))
          (json/encode {:text (str " " (last match))})
          (json/encode {:text "NO VPN CONN" :state "Critical" :class "down"}))]
    (printf "%s" out-str)))


(defn waybar-notification-status []
  (let [is-paused (-> (shell {:out :string} "dunstctl is-paused")
                      :out
                      str/trim
                      Boolean/parseBoolean)]
    (if is-paused
      (printf " ")
      (printf " "))))


(comment
  ;; no can do
  (slurp "/proc/loadavg")
      

  (-> (shell {:out :string} "dunstctl is-paused")
      :out
      str/trim
      Boolean/parseBoolean)

  ;;
  )


(let [action (first *command-line-args*)]
  (case action
    "date" (waybar-date)
    "disk" (waybar-disk)
    "licht" (waybar-licht)
    "load" (waybar-load)
    "memory" (waybar-memory)
    "music" (waybar-music)
    "notification-status" (waybar-notification-status)
    "toggle" (waybar-toggle)
    "vpn" (waybar-vpn)
    (default)))
