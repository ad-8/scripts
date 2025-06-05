#!/usr/bin/env sh

# example URLs:
# https://github.com/babashka/babashka/releases/download/v1.12.200/babashka-1.12.200-linux-amd64-static.tar.gz
# https://github.com/babashka/babashka/releases/download/v1.12.200/babashka-1.12.200-linux-amd64-static.tar.gz.sha256

# Fetch the latest release tag name from GitHub API
LATEST_TAG=$(curl -s https://api.github.com/repos/babashka/babashka/releases/latest | jq -r .tag_name)
VERSION="${LATEST_TAG#v}"  # Strip the 'v' prefix

echo "Installed version = $(bb --version | awk '{print $2}')"
echo
echo "Latest tag     = $LATEST_TAG"
echo "Latest version = $VERSION"

#exit 0

# File and URL names
TAR_FILE="babashka-${VERSION}-linux-amd64-static.tar.gz"
SHA_FILE="${TAR_FILE}.sha256"
BASE_URL="https://github.com/babashka/babashka/releases/download/${LATEST_TAG}"

# Download the tarball and its SHA256 file
echo "Downloading binary from Github ..."
wget --quiet "${BASE_URL}/${TAR_FILE}"
wget --quiet "${BASE_URL}/${SHA_FILE}"

# GitHub SHA256 file contains only the hash, not the filename, so rewrite it
EXPECTED_HASH=$(cat "$SHA_FILE")
echo "${EXPECTED_HASH}  ${TAR_FILE}" > "${SHA_FILE}.formatted"

# Verify the SHA256 checksum
echo
echo "---------------------"
echo "Verifying checksum..."
sha256sum -c "${SHA_FILE}.formatted"
echo "---------------------"
echo

# Extract the archive
echo "Extracting..."
tar xzf "$TAR_FILE"

# Install the binary
echo "Installing bb to /usr/bin (sudo required)..."
sudo mv bb /usr/bin/
sudo chmod +x /usr/bin/bb

# Clean up
rm "$TAR_FILE" "$SHA_FILE" "${SHA_FILE}.formatted"

# Verify installation
echo "Successfully updated babashka:"
bb --version
