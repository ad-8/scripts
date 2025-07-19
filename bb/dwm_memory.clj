#!/usr/bin/env bb
(ns dwm-memory
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]
            [cheshire.core :as json]))

(defn get-hostname []
  (let [output (:out (shell {:out :string} "hostnamectl"))
        ;; regex to capture the hostname after "Static hostname: "
        hostname (re-find #"Static hostname:\s*(\S+)" output)]
    (if hostname
      (second hostname)
      "unknown-hostname")))

(defn determine-css-class [hostname used]
  (case hostname
    "ax-mac"  (if (> used 6.0) :low :ok)
    "ax-fuji" (if (> used 12.5) :low :ok)
    (if (> used 6.0) :low :ok)))

(defn memory [output-fmt]
  (let [cmd "free -h | awk '/^Mem/ { print $3 \"/\" $2 }' | sed 's/i//g' | sed 's/G//'"
        mem (-> (shell {:out :string} "/bin/bash" "-c" cmd)
                :out
                str/trim)
        [used _total] (str/split mem #"/")
        class (determine-css-class (get-hostname) (Float/parseFloat used))
        fmt (format "󰍛 %s" mem)
        json (json/encode {:text fmt :class class})]
    (if (= "json" output-fmt)
      (println json)
      (println fmt))))

(defn run []
  (let [args *command-line-args*]
    (if (= "json" (first args))
      (memory "json")
      (memory "text"))))

(run)