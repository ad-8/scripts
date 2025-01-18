#!/usr/bin/env bb
(ns vpn2
  (:require [babashka.process :refer [shell]]))


;; sadly, protonvpn-cli is dead on arch linux


; match e.g.: ["ProtonVPN DE#316" "DE#316"]
(let [match (->> (shell {:out :string} "nmcli con show")
                 :out
                 (re-find #"ProtonVPN (\w+#\d+)"))]
  (if (some? match)
    (println "" (last match))
    (println "NO VPN CONN")))
