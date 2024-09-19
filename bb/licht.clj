#!/usr/bin/env bb
(ns licht
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.edn]
            [babashka.process :refer [shell process]]))


(defn light-screen [brightness]
  (sh "light" "-S" (str brightness)))

; light -L lists available devices
(defn light-keyboard [brightness]
  (sh "light" "-s" "sysfs/leds/smc::kbd_backlight" "-S" (str brightness)))

(defn get-light-screen []
  (-> (sh "light" "-G") :out (str/trim)))

(defn get-light-keyboard []
  (-> (sh "light" "-s" "sysfs/leds/smc::kbd_backlight" "-G") :out (str/trim)))

(defn set-ext-brightness [val]
  (sh "ddcutil" "setvcp" "10" (str val)))

(defn set-ext-contrast [val]
  (sh "ddcutil" "setvcp" "12" (str val)))

(defn get-ext-vals []
  (let [brigh (-> (sh "ddcutil" "getvcp" "10") :out)
        cont  (-> (sh "ddcutil" "getvcp" "12") :out)
        brightness (last (re-find  #"\bcurrent value =\s*(\d+)\b" brigh))
        contrast   (last (re-find  #"\bcurrent value =\s*(\d+)\b" cont))]
    (format "Brightness: %s\nContrast:   %s\n"  brightness contrast)))

(defn set-color-temp
  "Usage: sct [temperature]
   Temperatures must be in a range from 1000-10000
   If no arguments are passed sct resets the display to the default temperature (6500K)"
  [n]
  (sh "sct" (str n)))



(defn heading [s] 
  (let [line (apply str (repeat (count s) "-"))]
    (format "%s\n%s\n%s" line s line)))


(defn print-all-the-light-we-can-see []
  (let [disp (get-light-screen)
        keyb (get-light-keyboard)
        ext  (get-ext-vals)]
    (printf "%s\nDisplay:  %s\nKeyboard: %s" (heading "Internal") disp keyb)
    (printf "\n\n%s\n%s" (heading "External") ext)))

(defn illuminate! [int-b key-b ext-b ext-c col-t]
  (light-screen int-b) (light-keyboard key-b)
  (set-ext-brightness ext-b) (set-ext-contrast ext-c)
  (set-color-temp col-t))


(def settings {"aus"  {:name "AUS"
                       :vals [0 0 0 0 0]}
               "hi"   {:name "High"
                       :vals [80 0 80 80 6000]}
               "hi2"  {:name "High2"
                       :vals [67 67 67 67 6000]}
               "lo"   {:name "Low"
                       :vals [23 25 40 33 3750]}
               "ul1"  {:name "Ultra Low"
                       :vals [20 20 35 25 3000]}
               "ul2"  {:name "Ultra Low"
                       :vals [10 10 15 15 3000]}
               "ni"  {:name "night"
                       :vals [2 2 15 15 3000]}
               "max"  {:name "Max"
                       :vals [100 0 100 100 6500]}
               "max-e" {:name "Max External"
                        :vals [50 0 100 100 6500]}
               "med" {:name "Medium"
                      :vals [50 50 50 50 4250]}
               "kl"   {:name "Kino Low"
                       :vals [0 5 50 40 3333]}
               "kl2"   {:name "Kino Low 2"
                        :vals [0 5 25 25 3333]}
               "kh"   {:name "Kino High"
                       :vals [0 5 80 80 6000]}
               "km"   {:name "Kino Max"
                       :vals [0 0 100 100 6500]}})


(defn ask-user
  "Lets the user choose a setting interactively via dmenu."
  []
  (let [session-type (-> (shell {:out :string} "/bin/bash -c" "echo $XDG_SESSION_TYPE") :out str/trim)
        menu-cmd (if (= "wayland" session-type) "wmenu" "dmenu")]
    (-> (process "echo" "-e" (str/join "\n" (into (sorted-map) settings)))
        (process {:out :string} menu-cmd "-i" "-l" "15" "-p" "licht")
        deref :out str/trim clojure.edn/read-string first)))


(defn set-lights! [first-arg]
  (let [valid-arg (get settings first-arg)
        user-choice (if valid-arg first-arg (ask-user))
        selected-value (get settings user-choice)
        ntfy (format "notify-send Licht %s --app-name dwm-licht --expire-time 4000 --icon 
                      brightness-high-symbolic --replace-id 126" (:name selected-value))]
    (apply illuminate! (:vals selected-value))
    (spit "/tmp/licht-curr-val" (str user-choice "\n"))
    (shell ntfy)))


(let [fst (first *command-line-args*)]
  (if (= "get" fst)
    (print-all-the-light-we-can-see)
    (set-lights! fst)))
