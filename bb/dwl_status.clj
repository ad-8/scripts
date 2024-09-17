#!/usr/bin/bb
(ns dwl-status
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))

(import 'java.time.LocalDateTime
        'java.time.format.DateTimeFormatter)


(def formatter (DateTimeFormatter/ofPattern "dd.MM. HH:mm:ss"))
(def fmt-sec (DateTimeFormatter/ofPattern "ss"))


#_(shell "/home/ax/scripts/bb/i3vpn.clj")



; dwlb -status all 'text ^bg(ff0000)^lm(foot)text^bg()^lm() text'
(while true
  (let [now (LocalDateTime/now)
        date (.format now formatter)
        sec (Integer/parseInt (.format now fmt-sec))
        vpn (when (= 0 (mod sec 10)) (->> (shell {:out :string} "/home/ax/scripts/bb/i3vpn.clj dwm") :out str/split-lines first))
        space (when (= 0 (mod sec 10)) (->> (shell {:out :string} "/home/ax/scripts/bb/dwm_disk_space.clj") :out str/split-lines first))
        fmt (format "dwlb -status all '^fg(ff0000)^lm(foot) foo-err^bg()^fg()^lm() | bla | %s | %s | %s '" 
                    vpn space date)
        
        ;
        ]
    (if (= 0 (mod sec 10))
      (shell fmt)
      (Thread/sleep 1000))))

