#!/usr/bin/env sh

echo "Total lines:\n---------"
fd . -u --type f \
  --exclude .git \
  --exclude alacritty \
  --exclude emoji \
  --exclude font-awesome \
  ~/dotfiles/ \
  -x cat {} | wc -l

echo -e "\nTop 10 files by line count:"
fd . -u --type f \
  --exclude .git \
  --exclude alacritty \
  --exclude emoji \
  --exclude font-awesome \
  ~/dotfiles/ \
  -x sh -c 'wc -l "$0"' {} \; | sort -nr | head -n 10

