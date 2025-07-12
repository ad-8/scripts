#!/usr/bin/env bb
(ns ss
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))

(defn timestamp []
  (-> (shell {:out :string} "date +%F_%H-%M-%S")
      :out str/trim))

(defn filename []
  (str (System/getProperty "user.home")
       "/sync/screenshots/"
       (timestamp)
       ".png"))

(defn notify [file]
  (shell "notify-send" "Screenshot saved" (str "(by clj) to " file)))

; grim ~/sync/screenshots/$(date +%F_%H-%M-%S).png 
(defn screenshot-full []
  (let [file (filename)
        result (shell "grim" file)]
    (when (= 0 (:exit result))
      (notify file))))

; grim -g "$(slurp)" ~/sync/screenshots/$(date +%F_%H-%M-%S).png
(defn screenshot-area []
  (let [file (filename)
        geo  (-> (shell {:out :string} "slurp") :out str/trim)
        result (shell "grim" "-g" geo file)]
    (println "the geo is" geo)
    (when (= 0 (:exit result))
      (notify file))))

(case (first *command-line-args*)
  "full" (screenshot-full)
  "area" (screenshot-area)
  (println "Usage: bb ss.clj [full|area]"))
