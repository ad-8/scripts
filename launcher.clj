(ns launcher
  (:require  [clojure.string :as str]
             [clojure.edn]
             [babashka.fs :as fs]
             [babashka.process :refer [process shell]]))

;; a simple script to /somewhat/ replace keychords,
;; which are still missing in niri

(def cmds
  {"bluetui (TUI)" "alacritty -T ax-bluetui -e bluetui"
   "wiremix (TUI)" "alacritty -T ax-wiremix -e wiremix"
   "brave" "brave"
   "emacsclient" "emacsclient -c"
   "firefox" "firefox"
   "thunar (file manager)" "thunar"
   "volume control (pavu)" "pavucontrol"
   "toggle bluetooth on/off" "rfkill toggle bluetooth"
   "rofi - files" "rofi -show recursivebrowser"
   "rofi - windows" "rofi -show window"
   "linkding std" "bb /home/ax/x/ax_bookmarks.clj std"
   "linkding archived" "bb /home/ax/x/ax_bookmarks.clj archived"
   "kill a process" "sh -c 'ps -u $USER -o pid,comm,args,%cpu,%mem | wmenu -i -l 10 -p Kill: | awk \"{print \\$1}\" | xargs -r kill'"})

(def providers {:rofi "rofi -dmenu -p mode-open"
                :wmenu (str "wmenu -i 
                                -l 20 
                                -f \"Hack Nerd Font 11\" 
                                -N \"#0c1014\"
                                -n \"#99d1ce\"
                                -M \"#0a3749\"
                                -m \"#99d1ce\"
                                -S \"#195466\"
                                -s \"#99d1ce\"
                                -p \"          Select action          \"")})

(let [user-choice (-> (process "echo -e" (str/join "\n" (sort (keys cmds))))
                      (process {:out :string} (:wmenu providers))
                      deref :out str/trim)
      cmd-to-run (get cmds user-choice)]
  (println user-choice "---" cmd-to-run)
  (shell cmd-to-run))

