#!/usr/bin/env sh

weather=$($HOME/my/scripts/bb/weather.clj long)

dunstify "Weather by i3weather" "$weather" -i /usr/share/icons/Papirus/32x32/apps/weather.svg -a "i3w"
