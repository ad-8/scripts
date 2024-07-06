#!/usr/bin/env bb
(ns ntfy
  (:require [babashka.http-client :as http]))

(def url "https://ntfy.sh/test_03a1842c-6c28-4bfc-8f0b-b913ef70fe7b")
(def headers {:title "mbp.unix"
              :tags "desktop_computer"})

(defn ntfy [msg]
  (let [resp (http/post url {:body msg :headers headers})]
    (if (= 200 (:status resp))
      (println "notification sent:" msg)
      (println "error sending notification"))))

(let [[msg & ignored-args] *command-line-args*]
  (ntfy msg))
