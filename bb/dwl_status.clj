#!/usr/bin/bb
(ns dwl-status
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))

(import 'java.time.LocalDateTime
        'java.time.format.DateTimeFormatter)


;(def formatter (DateTimeFormatter/ofPattern "dd.MM. HH:mm:ss"))
(def formatter (DateTimeFormatter/ofPattern " dd.MM.  HH:mm"))
(def fmt-sec (DateTimeFormatter/ofPattern "ss"))

;; echo " $(cat /tmp/licht-curr-val)"

; (shell {:out :string} "cat /tmp/licht-curr-val")


(defn shell-out [cmd freq curr-sec]
  (if (= 0 (mod curr-sec freq))
    (->> (shell {:out :string} cmd) 
         :out str/split-lines first)
    "..."))


; dwlb -status all 'text ^bg(ff0000)^lm(foot)text^bg()^lm() text'
(while true
  (let [now (LocalDateTime/now)
        date (.format now formatter)
        sec (Integer/parseInt (.format now fmt-sec))

        vpn (shell-out "/home/ax/scripts/bb/i3vpn.clj dwm" 30 sec)
        weather (shell-out "/home/ax/scripts/bb/weather.clj dwm" 30 sec)
        space (shell-out "/home/ax/scripts/bb/dwm_disk_space.clj" 30 sec)
        licht (shell-out "cat /tmp/licht-curr-val" 30 sec)
        volume (shell-out "/home/ax/scripts/dwm-volume.sh" 1 sec)

        fmt (format
             "dwlb -status all '^fg(a3be8c)%s^fg(4c566a) | ^lm(pavucontrol)%s ^lm()|  %s^bg()^lm() | %s | %s | ^fg(81a1c1) %s '"
             weather volume licht vpn space date)

        ;
        ]
    (if (= 0 (mod sec 30))
      (shell fmt)
      (Thread/sleep 1000))))

