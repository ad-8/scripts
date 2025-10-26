#!/usr/bin/env bb
(ns waybar
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]))

(defn default []
  (println "error-unknown-action"))

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



(defn- stdout! [cmd]
  (-> (shell {:out :string} cmd) :out str/trim))

(defn- print-curr-playing []
  (let [status (stdout! "playerctl status")
        track-number (stdout! "playerctl metadata xesam:trackNumber")
        title (stdout! "playerctl metadata xesam:title")
        out-str (format "%s. %s" track-number title)]
    (if (= "Playing" status)
      (printf "%s" out-str)
      (printf "PAUSED %s" out-str))))

(def supported-players ["strawberry" "fooyin" "emms"])

(defn supported-player? [metadata] 
  (let [line (-> metadata str/split-lines first)]
    (some #(str/starts-with? line %) supported-players)))

(defn waybar-music []
  (let [metadata (stdout! "playerctl metadata")]
    (if (supported-player? metadata)
      (print-curr-playing)
      (printf "err-usp"))))

(defn waybar-toggle [minimal]
  ;; `:continue true` prevents the exception on non-zero exit codes.
  (let [status (-> (shell {:out :string :continue true} "sh -c 'pgrep waybar >/dev/null'") :exit)]
    (if (= 0 status)
      (do (println "waybar is running -> killing it")
          (let [status (-> (shell {:out :string} "sh -c 'pkill -f waybar'") :exit)]
            (printf "killing waybar, status = %d\n" status)))
      (do (println "not runnin'")
          (if minimal
            (let [status (-> (shell {:out :string} "sh -c 'setsid waybar -c ~/.config/waybar/config-minimal -s ~/.config/waybar/style-minimal.css >/dev/null 2>&1 &'") :exit)]
              (printf "killing waybar, status = %d\n" status))
            (let [status (-> (shell {:out :string} "sh -c 'setsid waybar >/dev/null 2>&1 &'") :exit)]
              (printf "killing waybar, status = %d\n" status)))))))


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


(let [action (first *command-line-args*)]
  (case action
    "date" (waybar-date)
    "disk" (waybar-disk)
    "licht" (waybar-licht)
    "load" (waybar-load)
    "memory" (waybar-memory)
    "music" (waybar-music)
    "notification-status" (waybar-notification-status)
    "toggle" (waybar-toggle false)
    "toggle-min" (waybar-toggle true)
    "vpn" (waybar-vpn)
    (default)))
