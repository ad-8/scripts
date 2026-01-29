(ns launcher
  (:require  [clojure.string :as str]
             [clojure.edn]
             [babashka.process :refer [process shell]]))

;; a simple script to /somewhat/ simulate keychords,
;; which are still missing in niri

(def cmds
  {"bluetui" "alacritty -T ax-bluetui -e bluetui"
   "brave" "brave"
   "emacsclient" "emacsclient -c"
   "firefox" "firefox"
   "thunar (file manager)" "thunar"
   "volume control (pavu)" "pavucontrol"
   "toggle bluetooth on/off" "rfkill toggle bluetooth"
   "rofi - files" "rofi -show recursivebrowser"
   "rofi - windows" "rofi -show window"
   "linkding std" "bb /home/ax/x/ax_bookmarks.clj std"
   "linkding archived" "bb /home/ax/x/ax_bookmarks.clj archived"})

(let [user-choice (-> (process "echo -e" (str/join "\n" (sort (keys cmds))))
                      (process {:out :string} "rofi -dmenu -p mode-open")
                      deref :out str/trim)
      cmd-to-run (get cmds user-choice)]
  (println user-choice "---" cmd-to-run)
  (shell cmd-to-run))

