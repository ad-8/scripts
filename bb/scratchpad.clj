(ns scratchpad
  (:require  [babashka.http-client :as http]
             [cheshire.core :as json]
             [clojure.string :as str]
             [clojure.walk]))

; ready to go
(defn happy-get [url]
  (->> (http/get url)
       :body
       json/decode
       clojure.walk/keywordize-keys))



(def base  (->> "/home/ax/.config/i3/config"
                slurp
                str/split-lines
                (filter #(str/includes? % "bindsym"))
                (remove #(str/starts-with? % "   "))
                (remove #(str/starts-with? % "       "))
                (remove #(str/includes? % "XF86"))
                (remove #(str/includes? % "workspace number $"))
                (remove #(str/includes? % (or "move" "focus")))
                sort

     ;;
                ))

(->> base
     (filter #(str/includes? (str/lower-case %) "shift")))

(->> base
     (filter #(str/includes? (str/lower-case %) "mod1")))

(->> base
     (filter #(str/includes? (str/lower-case %) "scripts/bb")))

(->> base
     (remove #(re-find #"(shift|mod1|scripts/bb)" (str/lower-case %))))
