#!/usr/bin/env sh

emacs --daemon &

sleep 5

emacsclient -c -F '((title . "emacs-scratchpad-todo"))' ~/sync/TODO.org
sleep 5
emacsclient -c -F '((title . "scratchmacs"))'
sleep 3
