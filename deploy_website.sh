#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material

set -ex

REPO="git@github.com:square/wire.git"
DIR=temp-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Generate the API docs
./gradlew -p wire-library \
    :wire-grpc-client:dokkaGfm \
    :wire-moshi-adapter:dokkaGfm \
    :wire-runtime:dokkaGfm \
    :wire-compiler:dokkaGfm \
    :wire-gson-support:dokkaGfm \
    :wire-java-generator:dokkaGfm \
    :wire-kotlin-generator:dokkaGfm \
    :wire-reflector:dokkaGfm \
    :wire-schema:dokkaGfm \
    :wire-swift-generator:dokkaGfm

# Fix *.md links to point to where the docs live under Mkdocs.
# Linux
# sed -i 's/docs\/wire_compiler.md/wire_compiler/' README.md
# OSX
sed -i "" 's/wire-library\/docs\/wire_compiler.md/wire_compiler/' README.md
sed -i "" 's/wire-library\/docs\/wire_grpc.md/wire_grpc/' README.md

# Copy in special files that GitHub wants in the project root.
cat README.md | grep -v 'project website' > wire-library/docs/index.md
cp CHANGELOG.md wire-library/docs/changelog.md
cp CONTRIBUTING.md wire-library/docs/contributing.md
cp RELEASING.md wire-library/docs/releasing.md

# Build the site and push the new files up to GitHub
cd wire-library
  mkdocs gh-deploy
cd ..

# Undo any file changes to be able to jump branches.
git checkout -- .

# Restore Javadocs from 2.x
git checkout gh-pages
git cherry-pick 0e1192aaa1d074c9703748fae100daef707218d4
git push --set-upstream origin gh-pages && git push

# Delete our temp folder
cd ..
rm -rf $DIR
