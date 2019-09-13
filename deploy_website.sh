#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material

set -ex

REPO="git@github.com:prjkt-io/android-commons.git"
DIR=temp-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Generate the API docs
if [ -n "$(command -v gradle)" ]; then
  gradle \
    :buildtools:dokka \
    :shell:dokka \
    :theme:dokka
else
  ./gradlew \
    :buildtools:dokka \
    :shell:dokka \
    :theme:dokka
fi

# Copy in special files that GitHub wants in the project root.
cat README.md | grep -v 'svg' >docs/index.md

# Build the site and push the new files up to GitHub
mkdocs gh-deploy

git checkout gh-pages
git push

# Delete our temp folder
cd ..
rm -rf $DIR
