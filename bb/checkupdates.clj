#!/usr/bin/env bb

(ns checkupdates
  (:require [clojure.string :as str]
            [clojure.pprint :refer [print-table]]
            [babashka.process :refer [sh]]))


(defn parse-line [line]
  (let [[pkg old new] (-> line
                          (str/replace #" ->" "")
                          (str/split #"\s"))]
    {:pkg pkg
     :old old
     :new new}))


;; sh works better than shell when working with empty :out or :err
(defn print-updates []
  (let [lines (-> (sh ["checkupdates"]) :out str/split-lines)]
    (if (= [""] lines)
      (println "no updates")
      (do (print-table (map parse-line lines))
          (printf "\n\nupdates: %d\n" (count lines))))))


(defn print-news []
  (let [res (-> (sh ["paru" "--show" "--news"]))]
    (if (= "" (:out res))
      (println (str/trim (:err res)))
      (println (str/trim (:out res))))))


(defn print-flatpak []
  (let [out (-> (sh ["flatpak" "remote-ls" "--updates"]) :out str/trim)]
    (printf "Flatpak:\n%s\n" (if (= "" out) "-" out))))

(defn print-rust []
  (let [out (-> (sh ["rustup" "check"]) :out str/trim)]
    (printf "Rust:\n%s\n" out)))


(print-updates)
(println "\n---\n")
(print-news)
(println "\n---\n")
(print-flatpak)
(println "\n---\n")
(print-rust)
