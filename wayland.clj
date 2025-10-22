#!/usr/bin/env bb
(ns wayland
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]))


(defn default []
  (println "error-unknown-action"))

(defn- stdout! [cmd]
  (-> (shell {:out :string} cmd) :out str/trim))

(defn- status! [cmd]
  (-> (shell {:out :string :continue true} "sh -c" cmd) :exit ))



(defn volume-up []
  (println "volume +")
  (let [status0 (status! "wpctl set-mute @DEFAULT_AUDIO_SINK@ 0")
        status1 (status! "wpctl set-volume -l 2.0 @DEFAULT_AUDIO_SINK@ 5%+")]
    (println "s0 and s1:" status0, status1)))


(defn volume-down []
  (println "volume -")
  (let [status0 (status! "wpctl set-mute @DEFAULT_AUDIO_SINK@ 0")
        status1 (status! "wpctl set-volume -l 2.0 @DEFAULT_AUDIO_SINK@ 5%-")]
    (println "s0 and s1:" status0, status1)))

(defn volume-mute []
  (println "volume MUTE")
  (let [status0 (status! "wpctl set-mute @DEFAULT_AUDIO_SINK@ toggle")]
    (println "s0:" status0)))

(defn volume [action]
  (println "action = " action)

  ;; set volume
  (case action
    :up (volume-up)
    :down (volume-down)
    :mute (volume-mute))

  ;; send notification

  )


(let [action (first *command-line-args*)]
  (case action
    "volume-up" (volume :up)
    "volume-down" (volume :down)
    "volume-mute" (volume :mute)
    (default)))