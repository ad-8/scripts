#!/usr/bin/env bb

(ns command-runner
  (:require [babashka.deps :as deps]
            [babashka.process :refer [shell]]
            [clojure.string :as str]
            [clj-yaml.core :as yaml]))

(deps/add-deps '{:deps {clj-commons/clj-yaml {:mvn/version "1.0.29"}}})

(def commands-data (yaml/parse-string (slurp "/home/ax/x/commands.yml")))

(defn flatten-commands [commands-data]
  (mapcat (fn [category-data]
            (map #(assoc % :category (:category category-data))
                 (get category-data :commands [])))
          commands-data))

(def all-commands
  (flatten-commands commands-data))


;; (println "all:")
;; (clojure.pprint/pprint commands-data)
;; (println "flat:")
;; (clojure.pprint/pprint all-commands)


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

(def actual-command-name
  (str/trim (second (str/split (user-select) #": " 2))))

(def selected-command
  (first (filter #(= (:name %) actual-command-name) all-commands)))

(println "Executing command:" (:name selected-command) "from category:" (:category selected-command))
(shell "sh" "-c" (:cmd selected-command))
