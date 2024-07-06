#!/usr/bin/env bb

(ns play-pause
  (:require [clojure.string :as str]
            [babashka.process :refer [shell]]))

; replace-id is just some random number (that does not interfere with other replacement ids)
(def ntfy-play "notify-send playerctl Playing... --app-name=play-pause --expire-time=2000 --icon exaile-play --replace-id=123 --urgency=low")
(def ntfy-pause "notify-send playerctl Pausing... --app-name=play-pause --expire-time=2000 --icon exaile-pause --replace-id=123 --urgency=low")
(def status (-> (shell {:out :string} "playerctl status")
                :out
                str/trim))

(defn set-player [new-state]
  (case new-state
    "play"  (and (shell ntfy-play) (shell "playerctl play"))
    "pause" (and (shell ntfy-pause) (shell "playerctl pause"))
    :error))

(case status
  "Playing" (set-player "pause")
  "Paused"  (set-player "play")
  :unknown-status)
