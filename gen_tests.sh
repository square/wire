#!/bin/bash

set -e

mvn clean package -pl wire-compiler -am -Dmaven.test.skip

cd wire-runtime
PROTOS=`find src/test/proto -name '*.proto' | sed 's|^src/test/proto/||'`

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  --enum_options=squareup.protos.custom_options.enum_value_option,squareup.protos.custom_options.complex_enum_value_option,squareup.protos.foreign.foreign_enum_value_option \
  ${PROTOS}

# GSON

cp src/test/proto-java/com/squareup/wire/protos/alltypes/*.java ../wire-gson-support/src/test/java/com/squareup/wire/protos/alltypes/

# NO OPTIONS

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.noOptions \
  --no_options \
  --enum_options=squareup.protos.custom_options.enum_value_option \
  ${PROTOS}

cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/Ext_custom_options.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/Ext_custom_options.java.noOptions
cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/FooBar.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/FooBar.java.noOptions
cp src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/MessageWithOptions.java \
   src/test/proto-java/com/squareup/wire/protos/custom_options/MessageWithOptions.java.noOptions

rm -r src/test/proto-java.noOptions

# REGISTRY

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.person.EmptyRegistry \
  person.proto

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.ProtoRegistry \
  simple_message.proto external_message.proto foreign.proto

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.one_extension.OneExtensionRegistry \
  one_extension.proto

# ANDROID

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.android \
  --android \
  person.proto

cp src/test/proto-java.android/com/squareup/wire/protos/person/Person.java \
   src/test/proto-java/com/squareup/wire/protos/person/Person.java.android

rm -r src/test/proto-java.android

# FULL

java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/proto-java.full \
  --full \
  all_types.proto

cp src/test/proto-java.full/com/squareup/wire/protos/alltypes/AllTypes.java \
   src/test/proto-java/com/squareup/wire/protos/alltypes/AllTypes.java.full
cp src/test/proto-java.full/com/squareup/wire/protos/alltypes/Ext_all_types.java \
   src/test/proto-java/com/squareup/wire/protos/alltypes/Ext_all_types.java.full

rm -r src/test/proto-java.full
