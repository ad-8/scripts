#!/usr/bin/env bb
(ns dwm-disk-space
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]))

(let [free-space-on-root (->> (shell {:out :string} "df -h / --output=avail")
                              :out
                              str/split-lines
                              last
                              str/trim)
      fmt (format "󰋊 %s" free-space-on-root)]
  (println fmt))
