#!/bin/bash

set -e

mvn clean package -pl wire-compiler -am -Dmaven.test.skip

cd wire-runtime
PROTOS=`find src/test/proto -name '*.proto' | sed 's|^src/test/proto/||' | sort`

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  google/protobuf/descriptor.proto \
  ${PROTOS}

# NO OPTIONS

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.noOptions \
  --excludes=google.protobuf.* \
  ${PROTOS}

cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/FooBar.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/FooBar.java.noOptions
cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/MessageWithOptions.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/MessageWithOptions.java.noOptions

# INCLUDES / EXCLUDES

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.pruned \
  --includes=squareup.protos.roots.A,squareup.protos.roots.H \
  --excludes=squareup.protos.roots.B \
  roots.proto

cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/A.java \
   src/test/proto-java/com/squareup/wire/protos/roots/A.java.pruned
cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/D.java \
   src/test/proto-java/com/squareup/wire/protos/roots/D.java.pruned
cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/E.java \
   src/test/proto-java/com/squareup/wire/protos/roots/E.java.pruned
cp src/test/proto-java.pruned/com/squareup/wire/protos/roots/H.java \
   src/test/proto-java/com/squareup/wire/protos/roots/H.java.pruned

# ANDROID

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.android \
  --android \
  person.proto

cp src/test/proto-java.android/com/squareup/wire/protos/person/Person.java \
   src/test/proto-java/com/squareup/wire/protos/person/Person.java.android

# ANDROID COMPACT

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.android.compact \
  --android \
  --compact \
  person.proto

cp src/test/proto-java.android.compact/com/squareup/wire/protos/person/Person.java \
   src/test/proto-java/com/squareup/wire/protos/person/Person.java.android.compact

# COMPACT

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.compact \
  --compact \
  all_types.proto

cp src/test/proto-java.compact/com/squareup/wire/protos/alltypes/AllTypes.java \
   src/test/proto-java/com/squareup/wire/protos/alltypes/AllTypes.java.compact

# GSON (uses COMPACT)

cp src/test/proto-java.compact/com/squareup/wire/protos/alltypes/AllTypes.java \
   ../wire-gson-support/src/test/java/com/squareup/wire/protos/alltypes/AllTypes.java
cp src/test/proto-java/com/squareup/wire/protos/RepeatedAndPacked.java \
   ../wire-gson-support/src/test/java/com/squareup/wire/protos/RepeatedAndPacked.java

rm -r src/test/proto-java.noOptions
rm -r src/test/proto-java.pruned
rm -r src/test/proto-java.compact
rm -r src/test/proto-java.android
rm -r src/test/proto-java.android.compact
