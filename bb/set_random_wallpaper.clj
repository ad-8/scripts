#!/usr/bin/bb
(ns set-random-wallpaper
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [babashka.process :refer [shell]]))



(def wallpaper-base (io/file (System/getProperty "user.home") "sync" "wallpapers" "keira"))

(comment
  (->> (shell/sh "ls" (str wallpaper-base))
       :out
       (str/split-lines)))

(def wallpaper-dir (io/file wallpaper-base))

(defn set-wallpaper-xorg [path]
  (let [feh (format "feh --bg-max \"%s\"" path)]
    (shell/sh "sh" "-c" feh)
    (println "Wallpaper changed.")))


(defn set-wallpaper-wayland [path]
  (let [feh (format "swaybg --image %s --mode fit" path)]
    ;; by default, swaybg just "stacks" the wallpapers
    (try (shell "killall swaybg") (catch Exception e))
    (Thread/sleep 250)
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
       (filter picture?)
       rand-nth))


(defn get-session-type []
  (-> (shell {:out :string} "/bin/bash -c" "echo $XDG_SESSION_TYPE") :out str/trim))


(defn set-wall [path]
  (if (= "wayland" (get-session-type))
    (set-wallpaper-wayland path)
    (set-wallpaper-xorg path)))


(-> wallpaper-dir
    random-wallpaper
    set-wall)
