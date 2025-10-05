#!/usr/bin/env sh

emacs --daemon &

sleep 5

emacsclient -c -F '((title . "emacs-scratchpad-todo"))' ~/sync/TODO.org 2>&1 >> ~/emacs-start.log & 
emacsclient -c -F '((title . "scratchmacs"))' 2>&1 >> ~/emacs-start.log &
