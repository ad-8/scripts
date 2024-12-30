#!/usr/bin/env bb
(ns vpn2
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))


;; sadly, protonvpn-cli is dead on arch linux


; match e.g.: ["ProtonVPN DE#316" "DE#316"]
(let [match (->> (shell {:out :string} "nmcli con show")
               :out
               (re-find #"ProtonVPN (\w+#\d+)"))]
  (if (some? match)
    (println "" (last match))
    (println "NO VPN CONN")))


(comment
  re-seq
  re-find

  (conj '(1 2 3) 4 5)
  (conj [1 2 3] 4 5)
  (conj #{1 2 3} 4 6)
  (assoc {:a 1 :b 2} :c 100 :d 300)

(defn run [cmd]
  (let [res (shell {:out :string} cmd)
        code (:exit res)]
    (if (= 0 code)
      (-> res :out str/split-lines)
      (format "failed to run %s" cmd))))


  (let [out (run "nmcli con show")
        fil (->> out (filter #(str/includes? % "ProtonVPN")))
      ;; (seq x) is the recommended idiom for testing if a collection is not empty
        out' (if (seq fil)
               (last (re-find #"ProtonVPN (\w+#\d+)" (first fil)))

               "NO VPN CONN")]
    (println "" out')))
