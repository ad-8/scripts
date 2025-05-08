#!/usr/bin/env bb

(ns checkupdates
  (:require [clojure.string :as str]
            [clojure.pprint]
            [babashka.process :refer [shell sh]]))


(defn parse-line [line]
  (let [parts (-> line
                  (str/replace #" ->" "")
                  (str/split #"\s"))]

    {:pkg (first parts)
     :old (second parts)
     :new (last parts)}))


(let [lines (-> (shell {:out :string} "checkupdates")
                :out
                str/split-lines)
      news (-> (sh ["paru" "--show" "--news"]) :err)]

  (clojure.pprint/print-table (map parse-line lines))
  (printf "\n\nupdates: %d\n" (count lines))
  (println "\n---\n")
  (println (str/trim news)))
