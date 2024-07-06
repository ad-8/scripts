#!/usr/bin/env sh

weather=$($HOME/my/scripts/bb/weather.clj dunst)

notify-send --app-name "i3w" --icon weather "Weather" "$weather"
