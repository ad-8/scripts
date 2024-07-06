#!/usr/bin/bb
(ns set-random-wallpaper
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def wallpaper-base (io/file (System/getProperty "user.home") "Pictures" "nord-background"))

(comment
  (->> (shell/sh "ls" (str wallpaper-base))
       :out
       (str/split-lines)))

(def wallpaper-dir (io/file wallpaper-base))

(defn set-wallpaper [path]
  (let [feh (format "feh --bg-fill \"%s\"" path)]
    (shell/sh "sh" "-c" feh)
    (println "Wallpaper changed.")))

(defn ends-with-extension? [^java.io.File f, ^String e]
  (let [file (.toLowerCase (.getName f))
        ext  (.toLowerCase e)]
    (.endsWith file ext)))

(defn picture?
  "Predicate Function that returns true if file is a picture."
  [f]
  (or (ends-with-extension? f ".jpg")
      (ends-with-extension? f ".jpeg")
      (ends-with-extension? f ".png")))


(defn random-wallpaper [dir]
  (->> dir
       file-seq
       (filter #(picture? %))
       rand-nth))

(-> wallpaper-dir
    random-wallpaper
    set-wallpaper)
