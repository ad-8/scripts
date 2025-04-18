#!/usr/bin/env bb

(ns weather
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



(def settings-file (io/file (System/getProperty "user.home") "my" "cfg" "weather.edn"))
(def weather-codes-file (io/file (System/getProperty "user.home") "my" "cfg" "weather-codes.json"))
(def weather-codes (-> weather-codes-file slurp json/decode clojure.walk/keywordize-keys))


(when-not (.exists settings-file)
  (println "failed to read weather.edn")
  (System/exit 1))


(def settings (-> settings-file slurp edn/read-string))
(def current-place (get-in settings [:locations (:curr-loc settings)]))


(def url "https://api.open-meteo.com/v1/forecast"),
(def query-params {:latitude (:lat current-place)
                   :longitude (:lon current-place)
                   :timezone "Europe/Berlin"
                   :forecast_days 3
                   :models "icon_seamless"
                   ;; all checkboxes checked for :daily
                   :daily ["weather_code", "temperature_2m_max", "temperature_2m_min", "apparent_temperature_max", "apparent_temperature_min", "sunrise", "sunset", "daylight_duration", "sunshine_duration", "precipitation_sum", "rain_sum", "showers_sum", "snowfall_sum", "precipitation_hours", "precipitation_probability_max", "wind_speed_10m_max", "wind_gusts_10m_max", "wind_direction_10m_dominant", "shortwave_radiation_sum", "et0_fao_evapotranspiration"]
                   ;; Every weather variable available in hourly data, is available as current condition as well.
                   :current ["weather_code" "is_day" "temperature_2m", "precipitation", "rain", "cloud_cover", "precipitation_probability"]
                   :hourly ["weather_code", "temperature_2m", "precipitation_probability", "precipitation", "cloud_cover"]})


(defn make-request [url query-params]
  (let [params {:query-params query-params}
        resp (try (http/get url params)
                  (catch Exception e (println (.getMessage e))))]
    (if (= 200 (:status resp))
      resp
      (System/exit 1))))


(defn sunshine_hrs [data]
  (->> data :daily :sunshine_duration
       (map #(/ % 3600))
       (map #(format "%.1f" %))
       (map #(Double/parseDouble %))))


(defn format-number
  "Sometimes the temperature from the API is an even number like 4,
  which, when formatted with the format string used below, throws an exception
  when used with an integer, so we need to explicitly parse to a float"
  [num]
  (let [fmt (format "%.0f" (float num))]
    (if (= "-0" fmt) 
      "0" 
      fmt)))


(defn parse-day [daily-data i]
  {:dt (nth (:time daily-data) i)
   :sunrise (nth (:sunrise daily-data) i)
   :sunset (nth (:sunset daily-data) i)
   :weather-code (nth (:weather_code daily-data) i)
   :temp-min (format-number (nth (:temperature_2m_min daily-data) i))
   :temp-max (format-number (nth (:temperature_2m_max daily-data) i))
   :sunshine-duration (nth (:sunshine_duration daily-data) i)
   :wind_gusts_10m_max (nth (:wind_gusts_10m_max daily-data) i)
   :wind_speed_10m_max (nth (:wind_speed_10m_max daily-data) i)
   :precipitation-sum (nth (:precipitation_sum daily-data) i)})


(defn download-icon [url filename]
  (io/copy
   (:body (http/get url {:as :stream}))
   (io/file filename)))


(defn find-code [code]
  (let [code (-> code str keyword)]
    (-> (filter #(= code (first %)) weather-codes) first)))


(defn code-desc [code day?]
  (if day?
    (-> (find-code code) second :day :description)
    (-> (find-code code) second :night :description)))

(comment

(type weather-codes)
(code-desc 63 3)

  (find-code 71)

(clojure.pprint/pprint weather-codes)
(take 3 weather-codes)
(keys weather-codes)
(vals weather-codes)

(for)

;;
)


(defn code-img [code day?]
  (if day?
    (-> (find-code code) second :day :image)
    (-> (find-code code) second :night :image)))


(defn extract-time [dt]
  (-> dt (str/split #"T") last))


(defn sun-rise-set [today tomorrow]
  (let [sunset  (-> today :sunset extract-time)
        sunrise (-> tomorrow :sunrise extract-time)]
    (format "↓%s ↑%s" sunset sunrise)))


(defn fmt [day]
  (format "%2d %2d %s .. %.0f/%.0f km/h"
          (Integer/parseInt (:temp-min day))
          (Integer/parseInt (:temp-max day))
          (code-desc (:weather-code day) true)
          (:wind_speed_10m_max day)
          (:wind_gusts_10m_max day)))


(defn forecast [data]
  (let [curr (:current data)
        curr-temp (-> curr :temperature_2m format-number)
        day? (if (= 0 (:is_day curr)) false true)
        curr-desc (code-desc (:weather_code curr) day?)
        curr-icon (code-img (:weather_code curr) day?)
        daily-data (:daily data)
        today   (parse-day daily-data 0)
        today+1 (parse-day daily-data 1)
        today+2 (parse-day daily-data 2)
        ;; {:exit 1, :out , :err Unknown option -3°C  Foggy  (LOCATION)
        ;; if the temp is negative and the whole string starts with -3°C, 
        ;; it is interpreted as a shell argument like -y and causes an error with notify-send
        fmt-old (format "   %s°C  %s  (%s)\n\n%s\n\ntoday:     %s\ntomorrow:  %s\nday after: %s"
                        curr-temp curr-desc (:short current-place)
                        (sun-rise-set today today+1)
                        (fmt today)
                        (fmt today+1) (fmt today+2))
        shell-txt (format "notify-send --app-name \"%s\" --icon \"%s\" Weather \"%s\""
                          "dunst-weather" (:icon-path settings) fmt-old)]

    (download-icon curr-icon (:icon-path settings))
    (shell shell-txt)))


(defn print-for-i3bar-short [status curr-temp curr-desc]
  (if (= 200 status)
    (format "%s°C %s" curr-temp curr-desc)
    (format "Request Error: status code %d" status)))


(defn dwmblocks [data wm]
  (let [location (:shortkw current-place)
        curr (:current data)
        day? (if (= 0 (:is_day curr)) false true)
        curr-temp (-> curr :temperature_2m format-number)
        curr-desc (code-desc (:weather_code curr) day?)
        weather (print-for-i3bar-short 200 curr-temp curr-desc)
        fmt (case wm
              "dwm" (format "%s %s" weather location)
              "i3" (json/encode {:text (format "%s°C %s" curr-temp curr-desc)}))]
    fmt))


(let [arg1 (first *command-line-args*)
      resp (make-request url query-params)
      data (-> resp :body json/decode keywordize-keys)
      output (case arg1
               "dunst" (forecast data)
               "dwm" (dwmblocks data "dwm")
               "i3" (dwmblocks data "i3")
               ":invalid-argument")]
  (println output))


(comment 
  (def res (make-request url query-params))
  (def data (->> res
                 :body
                 json/decode
                 keywordize-keys))

  data
  (keys data)
  (get data :daily)
  (:current data)

  (try (forecast data)
       (catch Exception e (str "error: " (.getMessage e))))

  (dwmblocks data "dwm")

  (try
    (/ 1 0)
    (catch Exception e (str "caught exception: " (.getMessage e))))

  )


(comment
  ;; better safe than sorry :)
  (def daily-data-for-test
    {:sunset ["2024-09-12T19:32" "2024-09-13T19:30" "2024-09-14T19:28"],
     :precipitation_hours [9.0 22.0 24.0],
     :wind_direction_10m_dominant [225 320 279],
     :daylight_duration [45907.84 45700.86 45493.08],
     :showers_sum [0.0 0.4 0.3],
     :snowfall_sum [0.0 0.0 0.0],
     :et0_fao_evapotranspiration [1.09 0.8 0.57],
     :precipitation_sum [5.5 16.1 57.6],
     :time ["2024-09-12" "2024-09-13" "2024-09-14"],
     :weather_code [61 61 63],
     :sunrise ["2024-09-12T06:47" "2024-09-13T06:48" "2024-09-14T06:50"],
     :sunshine_duration [0.0 0.0 0.0],
     :wind_gusts_10m_max [22.0 36.7 42.1],
     :shortwave_radiation_sum [6.29 4.18 1.87],
     :rain_sum [5.5 15.4 57.0],
     :apparent_temperature_min [4.4 4.8 1.7],
     :apparent_temperature_max [9.9 7.1 5.2],
     :wind_speed_10m_max [9.7 12.4 18.0],
     :temperature_2m_max [11.4 9.9 7.7],
     :precipitation_probability_max [100 100 100],
     :temperature_2m_min [6.9 6.6 5.7]})

  (deftest test-parse-day
    (are
     [input i expected]
     (= expected (parse-day input i))
      daily-data-for-test 0 {:dt "2024-09-12",
                             :sunrise "2024-09-12T06:47",
                             :sunset "2024-09-12T19:32",
                             :weather-code 61,
                             :temp-min "7",
                             :temp-max "11",
                             :sunshine-duration 0.0,
                             :precipitation-sum 5.5
                             :wind_gusts_10m_max 22.0
                             :wind_speed_10m_max 9.7}
      daily-data-for-test 1 {:dt "2024-09-13",
                             :sunrise "2024-09-13T06:48",
                             :sunset "2024-09-13T19:30",
                             :weather-code 61,
                             :temp-min "7",
                             :temp-max "10",
                             :sunshine-duration 0.0,
                             :precipitation-sum 16.1
                             :wind_gusts_10m_max 36.7
                             :wind_speed_10m_max 12.4}
      daily-data-for-test 2 {:dt "2024-09-14",
                             :sunrise "2024-09-14T06:50",
                             :sunset "2024-09-14T19:28",
                             :weather-code 63,
                             :temp-min "6",
                             :temp-max "8",
                             :sunshine-duration 0.0,
                             :precipitation-sum 57.6
                             :wind_gusts_10m_max 42.1
                             :wind_speed_10m_max 18.0}))

  (run-tests)
  ;;
  )


(comment

  (def data (->> (make-request url query-params)
                 :body
                 json/decode
                 keywordize-keys))
  (keys data)
  (:current_units data)
  (:hourly_units data)

  (:current data)
  data
  (keys (:hourly data))
  (:daily data)
  (forecast data)

  (def daily (:daily data))
  (def hourly (:hourly data))

  (->> (keys daily) sort)
  (into (sorted-map) daily)
  (into (sorted-map) (:daily_units data))

  (parse-day daily 1)
  (:precipitation_hours daily)

  (into (sorted-map) daily)

  (partition 8 (interleave
                (-> data :daily :time)
                (-> data :daily :sunrise)
                (-> data :daily :sunset)
                (-> data :daily :weather_code)
                (:temperature_2m_min daily)
                (:temperature_2m_max daily)
                (:sunshine_duration daily)
                (:precipitation_sum daily)))


  :hourly ["weather_code", "temperature_2m", "precipitation_probability", "precipitation", "cloud_cover"]
  (partition 6 (interleave
                (-> data :hourly :time)
                (-> data :hourly :weather_code)
                (:temperature_2m hourly)
                (:precipitation hourly)
                (:precipitation_probability hourly)
                (:cloud_cover hourly)))
  ;;
  )


(comment
  (def outmap {:time (-> data :hourly :time)
               :temp (-> data :hourly :temperature_2m)
               :prec (-> data :hourly :precipitation)
               :prec_prob (-> data :hourly :precipitation_probability)
               :sunshine_hrs (flatten (map (fn [x] (repeat 24 x)) (sunshine_hrs data)))
               :loc (:long current-place)})

  outmap
  (spit "/tmp/open-meteo.json" (json/encode outmap))
  (shell "/home/ax/my/code/python/weather-plot/venv/bin/python" "/home/ax/my/code/python/weather-plot/main.py"))
