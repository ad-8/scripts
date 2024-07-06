#!/usr/bin/bb

(ns i3weather
  (:require  [babashka.http-client :as http]
             [cheshire.core :as json]
             [clojure.edn :as edn]
             [clojure.java.io :as io]
             [clojure.walk]))

(import 'java.time.format.DateTimeFormatter)

(def script-dir (io/file (System/getProperty "user.home") "my" "scripts" "bb"))

(def places (->> (io/file script-dir "weather_locations.edn")
                 slurp
                 edn/read-string))

(def appid (->> (io/file script-dir "weather_key.edn")
                slurp
                edn/read-string
                :appid))

(def current-place (:h places))


(defn timestamp->datetime
  "Converts a unix timestamp (in seconds) to a java.time.ZonedDateTime object
   with the local (system default) time zone."
  [ts]
  (let [instant (java.time.Instant/ofEpochSecond ts)
        zone-id (java.time.ZoneId/systemDefault)
        zoned-dt (.atZone instant zone-id)]
    zoned-dt))


;; https://openweathermap.org/api/one-call-api
(defn make-request []
  (let [url    "https://api.openweathermap.org/data/2.5/onecall"
        params {:query-params {:appid   appid
                               :lat     (:lat current-place)
                               :lon     (:lon current-place)
                               :units   "metric"
                               :exclude "minutely,hourly"}}]
    (http/get url params)))


(comment
 (let [url    "https://api.openweathermap.org/data/2.5/onecall"
        params {:query-params {:appid   appid
                               :lat     (:lat current-place)
                               :lon     (:lon current-place)
                               :units   "metric"
                               :exclude "minutely,hourly"}}]
    (http/get url params)) 
  )

(defn format-number
  "Sometimes the temperature from the API is an even number like 4,
  which, when formatted with the format string used below, throws an exception
  when used with an integer, so we need to explicitly parse to a float"
  [num]
  (format "%.0f" (float num)))

(defn parse-day [d]
  (let [dt  (get d :dt)
        min (get-in d [:temp :min])
        max (get-in d [:temp :max])
        min-fmt (format-number min)
        max-fmt (format-number max)
        main (-> d :weather first :main)
        desc (-> d :weather first :description)
        d-fmt (format "%s %s %s" min-fmt max-fmt desc)]

    {:dt dt     :summary d-fmt
     :min min   :min-fmt min-fmt
     :max max   :max-fmt max-fmt
     :main main :desc desc}))

(defn dt->hhmm
  "Formats a ZonedDateTime object as HH:MM"
  [zdt]
   ; time.format(DateTimeFormatter.ofPattern("HH:mm")); / ; sadly doesn't round up the minute if sec >= 30
  (.format zdt (DateTimeFormatter/ofPattern "HH:mm")))

(defn sun-rise-set [fs]
  (let [sunset  (-> fs first :sunset dt->hhmm)
        sunrise (-> fs second :sunrise dt->hhmm)]
    (format "↓%s ↑%s" sunset sunrise)))

(defn parse-response [resp]
  (let [body (->> resp
                  :body
                  (json/parse-string)
                  (map #(clojure.walk/keywordize-keys %)))
        current (update (->> body     ; seq, count = 6: ("lat" "lon" "timezone" "timezone_offset" "current" "daily")
                             (drop 4)
                             (take 1)
                             first
                             second) :dt timestamp->datetime)
        forecast (->> body
                      (drop 5)
                      first
                      (drop 1)
                      flatten ; lazy seq of maps, count=8 (today 12:00 forecast, tomorrow 12:00 forecast, etc.)
                      (map #(update % :dt timestamp->datetime))
                      (map #(update % :sunrise timestamp->datetime))
                      (map #(update % :sunset timestamp->datetime))
                      (map #(update % :moonrise timestamp->datetime))
                      (map #(update % :moonset timestamp->datetime)))
        temps-next-3-days (->> forecast
                               (drop 1) ; today 12:00
                               (take 3) ; next 3 days, 12:00
                               (map #(:temp %)))
        min (->> temps-next-3-days
                 (apply min-key :min)
                 :min
                 format-number)
        max (->> temps-next-3-days
                 (apply max-key :max)
                 :max
                 format-number)
        curr-temp (-> current :temp format-number)
        curr-desc (-> current :weather first :description)
        today+1 (->> forecast (drop 1) (take 1) first parse-day)
        today+2 (->> forecast (drop 2) (take 1) first parse-day)]

    {:status (:status resp), :curr-temp curr-temp, :curr-desc curr-desc,
     :sun (sun-rise-set forecast) :body body
     :forecast forecast, :today+1 today+1, :today+2 today+2}))

; JSON FORMAT for i3status-rs
; {"icon": "...", "state": "...", "text": "...", "short_text": "..."}
(defn print-for-i3bar [{:keys [status curr-temp curr-desc today+1 today+2 sun]}]
  (if (= 200 status)

    {:text (format "%s°C  %s  %s • %s • %s • %s"
                   curr-temp curr-desc (:short current-place)
                   (:summary today+1) (:summary today+2)
                   sun)
     :short_text (format "%s°C   %s   %s"
                         curr-temp curr-desc (:short current-place))}

    (printf "Error: status code %d\n" status)))


(defn print-for-i3bar-short [{:keys [status curr-temp curr-desc today+1 today+2 sun]}]
  (if (= 200 status)

     {:text (format "%s°C %s"
                         curr-temp curr-desc)}

    (printf "Error: status code %d\n" status)))

(defn foo-long []
  (-> (make-request)
      (parse-response)
      (print-for-i3bar)
      (json/encode)))

(defn foo-short []
  (-> (make-request)
      (parse-response)
      (print-for-i3bar-short)
      (json/encode)))

(defn dwmblocks []
  (-> (make-request)
      (parse-response)
      (print-for-i3bar-short)
      :text
      println))

(let [args *command-line-args*
      arg1 (first args)
      output (case arg1
               "long"   (foo-long)
               "short"  (foo-short)
               "dwm"    (dwmblocks)
               ":invalid-argument")]
   (println output))
