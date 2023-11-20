#!/bin/bash

# Commit 0e1192aaa1d074c9703748fae100daef707218d4 contains Javadoc for Wire 2.x. Those should be
# present on gh-pages and published along with the other website content, but if for some reason
# they have to be re-added to gh-pages - run this script locally.

set -ex

DIR=temp-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone . $DIR

# Move working directory into temp folder
cd $DIR

# Restore docs from 2.x
git checkout 0e1192aaa1d074c9703748fae100daef707218d4
mkdir -p ../site
mv ./2.x ../site/2.x

# Delete our temp folder
cd ..
rm -rf $DIR
