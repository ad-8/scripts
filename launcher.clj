(ns launcher
  (:require  [clojure.string :as str]
             [clojure.edn]
             [babashka.process :refer [shell process]]))


(def cmds
  {
   "bluetui" "alacritty -T ax-bluetui -e bluetui"
   "brave" "brave"
   "emacsclient" "emacsclient -c"
   "firefox" "firefox"
   "thunar (file manager)" "thunar"
   })


(let [user-choice (-> (process "echo" "-e" (str/join "\n" (into (sorted-map) cmds)))
                      (process {:out :string} "wmenu -i -l 15")
                      deref :out str/trim clojure.edn/read-string first)
      cmd-to-run (get cmds user-choice)]
  (println user-choice "--" cmd-to-run)
  (shell cmd-to-run))
