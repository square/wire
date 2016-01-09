#!/bin/bash

set -e

./gradlew -p wire-compiler clean installDist

cd wire-runtime
PROTOS=`find src/test/proto -name '*.proto' | sed 's|^src/test/proto/||'`

../wire-compiler/build/install/wire-compiler/bin/wire-compiler \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  google/protobuf/descriptor.proto \
  ${PROTOS}

# NO OPTIONS

../wire-compiler/build/install/wire-compiler/bin/wire-compiler \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.noOptions \
  --excludes=google.protobuf.* \
  ${PROTOS}

cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/FooBar.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/FooBar.java.noOptions
cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/MessageWithOptions.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/MessageWithOptions.java.noOptions

# INCLUDES / EXCLUDES

../wire-compiler/build/install/wire-compiler/bin/wire-compiler \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.pruned \
  --includes=squareup.protos.roots.A \
  --excludes=squareup.protos.roots.B \
  roots.proto

cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/A.java \
   src/test/proto-java/com/squareup/wire/protos/roots/A.java.pruned
cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/D.java \
   src/test/proto-java/com/squareup/wire/protos/roots/D.java.pruned

# ANDROID

../wire-compiler/build/install/wire-compiler/bin/wire-compiler \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.android \
  --android \
  person.proto

cp src/test/proto-java.android/com/squareup/wire/protos/person/Person.java \
   src/test/proto-java/com/squareup/wire/protos/person/Person.java.android

# COMPACT

../wire-compiler/build/install/wire-compiler/bin/wire-compiler \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.compact \
  --compact \
  all_types.proto

cp src/test/proto-java.compact/com/squareup/wire/protos/alltypes/AllTypes.java \
   src/test/proto-java/com/squareup/wire/protos/alltypes/AllTypes.java.compact

# GSON (uses COMPACT)

cp src/test/proto-java.compact/com/squareup/wire/protos/alltypes/AllTypes.java \
   ../wire-gson-support/src/test/java/com/squareup/wire/protos/alltypes/AllTypes.java

rm -r src/test/proto-java.noOptions
rm -r src/test/proto-java.pruned
rm -r src/test/proto-java.compact
rm -r src/test/proto-java.android
