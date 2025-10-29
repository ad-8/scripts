#!/usr/bin/env bb

(ns command-runner
  (:require [babashka.deps :as deps]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [clj-yaml.core :as yaml]))

(deps/add-deps '{:deps {clj-commons/clj-yaml {:mvn/version "1.0.29"}}})

'({:category "Help", :commands ({:name "help - about R", :cmd "echo 'This is R, a simple command runner.'"})}
  {:category "System",
   :commands
   ({:name "Check disk usage", :cmd "df -h / && echo && df -h | grep -i nas"}
    {:name "toggle notifications (dunst)", :cmd "$HOME/scripts/dunst_toggle_and_notify.clj"})}
  {:etc :pp})
(def commands-data (yaml/parse-string (slurp "/home/ax/x/commands.yml")))

(defn flatten-commands [commands-data]
  (mapcat (fn [category-data]
            (map #(assoc % :category (:category category-data))
                 (get category-data :commands [])))
          commands-data))


'({:name "help - about R", :cmd "echo 'This is R, a simple command runner.'", :category "Help"}
  {:name "Check disk usage", :cmd "df -h / && echo && df -h | grep -i nas", :category "System"}
  {:name "toggle notifications (dunst)", :cmd "$HOME/scripts/dunst_toggle_and_notify.clj", :category "System"}
  {:and :more})
(def all-commands
  (flatten-commands commands-data))

'("Wttr: Show loss"
  "Wttr: Show data"
  "Wttr: Plot"
  "...")
(def command-names
  (->> all-commands
       (sort-by (juxt (comp str/lower-case :category)
                      (comp str/lower-case :name)))
       reverse
       (map #(str (:category %) ": " (:name %)))))

(defn user-select []
  (try (str/trim (:out (shell {:in (str/join "\n" command-names) :out :string} "fzf")))
       (catch Exception e ((println "No command selected" (.getMessage e))
                           (System/exit 0)))))

'"help - about R"
(def actual-command-name
  (str/trim (second (str/split (user-select) #": " 2))))

'#ordered/map ([:name help - about R] [:cmd echo 'This is R, a simple command runner.'] [:category Help])
(def selected-command
  (first (filter #(= (:name %) actual-command-name) all-commands)))

(println "Executing command:" (:name selected-command) "from category:" (:category selected-command))
(shell "sh" "-c" (:cmd selected-command))
