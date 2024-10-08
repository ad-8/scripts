#!/usr/bin/env bb
(ns colors)


;; Copyright © 2014 Thura Hlaing
;; Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

;; https://github.com/trhura/clojure-term-colors


(defn- escape-code
  [i]
  (str "\033[" i "m"))

(def ^:dynamic *colors*
  "foreground color map"
  (zipmap [:grey :red :green :yellow
           :blue :magenta :cyan :white]
          (map escape-code
               (range 30 38))))

(def ^:dynamic *highlights*
  "background color map"
  (zipmap [:on-grey :on-red :on-green :on-yellow
           :on-blue :on-magenta :on-cyan :on-white]
          (map escape-code
               (range 40 48))))

(def ^:dynamic *attributes*
  "attributes color map"
  (into {}
        (filter (comp not nil? key)
                (zipmap [:bold, :dark, nil, :underline,
                         :blink, nil, :reverse-color, :concealed]
                        (map escape-code (range 1 9))))))

(def ^:dynamic *reset* (escape-code 0))

;; Bind to true to have the colorize functions not apply coloring to
;; their arguments.
(def ^:dynamic *disable-colors* nil)

(defmacro define-color-function
  "define a function `fname' which wraps its arguments with
        corresponding `color' codes"
  [fname color]
  (let [fname (symbol (name fname))
        args (symbol 'args)]
    `(defn ~fname [& ~args]
       (if-not *disable-colors*
         (str (clojure.string/join (map #(str ~color %) ~args)) ~*reset*)
         (apply str ~args)))))

(defn define-color-functions-from-map
  "define functions from color maps."
  [colormap]
  (eval `(do ~@(map (fn [[color escape-code]]
                      `(println ~color ~escape-code)
                      `(define-color-function ~color ~escape-code))
                    colormap))))

(define-color-functions-from-map *colors*)
(define-color-functions-from-map *highlights*)
(define-color-functions-from-map *attributes*)


;; Available functions:
;; -------------------------------------------------------------------
;; white, cyan, magenta, blue, yellow, green, red, grey, on-white,
;; on-cyan, on-magenta, on-blue, on-yellow, on-green, on-red, on-grey,
;; concealed, reverse-color, blink, underline, dark, bold
;; -------------------------------------------------------------------


(println "Some text...\n")
(println (magenta "yohoho"))
(println)
(println (on-red (white "yohoho")))
(println)
(println (underline (on-blue (white "yohoho"))))
(println)
(println "Normal again")
