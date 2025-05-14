#!/usr/bin/env bb

(ns checkupdates
  (:require [clojure.string :as str]
            [clojure.pprint]
            [babashka.process :refer [sh]]))


(defn parse-line [line]
  (let [[pkg old new] (-> line
                          (str/replace #" ->" "")
                          (str/split #"\s"))]
    {:pkg pkg
     :old old
     :new new}))


(defn print-updates [lines]
  (if (= [""] lines)
    (println "no updates")
    (do (clojure.pprint/print-table (map parse-line lines))
        (printf "\n\nupdates: %d\n" (count lines)))))


;; sh works better than shell when working with empty :out or :err
(let [lines (-> (sh ["checkupdates"]) :out str/split-lines)
      news (-> (sh ["paru" "--show" "--news"]) :err)]

  (print-updates lines)
  (println "\n---\n")
  (println (str/trim news)))
