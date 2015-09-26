#!/bin/bash

set -e

mvn clean package -pl wire-compiler -am -Dmaven.test.skip

if [ -d temp_gen_tests ]; then
  rm -r temp_gen_tests
fi
mkdir temp_gen_tests
cp -R wire-runtime/src/test/proto/* temp_gen_tests/

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java \
  --enum_options=squareup.protos.custom_options.enum_value_option,squareup.protos.custom_options.complex_enum_value_option,squareup.protos.foreign.foreign_enum_value_option

# GSON

cp wire-runtime/src/test/proto-java/com/squareup/wire/protos/alltypes/* wire-gson-support/src/test/java/com/squareup/wire/protos/alltypes/

# NO OPTIONS

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java.noOptions \
  --no_options \
  --enum_options=squareup.protos.custom_options.enum_value_option

cp wire-runtime/src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/Ext_custom_options.java \
   wire-runtime/src/test/proto-java/com/squareup/wire/protos/custom_options/Ext_custom_options.java.noOptions
cp wire-runtime/src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/FooBar.java \
   wire-runtime/src/test/proto-java/com/squareup/wire/protos/custom_options/FooBar.java.noOptions
cp wire-runtime/src/test/proto-java.noOptions/com/squareup/wire/protos/custom_options/MessageWithOptions.java \
   wire-runtime/src/test/proto-java/com/squareup/wire/protos/custom_options/MessageWithOptions.java.noOptions
cp wire-runtime/src/test/proto-java.noOptions/com/squareup/wire/protos/foreign/Ext_foreign.java \
   wire-runtime/src/test/proto-java/com/squareup/wire/protos/foreign/Ext_foreign.java.noOptions

rm -r wire-runtime/src/test/proto-java.noOptions

# REGISTRY

rm -r temp_gen_tests
mkdir temp_gen_tests
cp wire-runtime/src/test/proto/person.proto temp_gen_tests/

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.person.EmptyRegistry

rm -r temp_gen_tests
mkdir temp_gen_tests
cp wire-runtime/src/test/proto/simple_message.proto temp_gen_tests/
cp wire-runtime/src/test/proto/external_message.proto temp_gen_tests/
cp wire-runtime/src/test/proto/foreign.proto temp_gen_tests/
cp -R wire-runtime/src/test/proto/google temp_gen_tests/

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.ProtoRegistry

rm -r temp_gen_tests
mkdir temp_gen_tests
cp wire-runtime/src/test/proto/one_extension.proto temp_gen_tests/

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java \
  --registry_class=com.squareup.wire.protos.one_extension.OneExtensionRegistry

# ANDROID

rm -r temp_gen_tests
mkdir temp_gen_tests
cp wire-runtime/src/test/proto/person.proto temp_gen_tests/

java -jar wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=temp_gen_tests \
  --java_out=wire-runtime/src/test/proto-java.android \
  --android

cp wire-runtime/src/test/proto-java.android/com/squareup/wire/protos/person/Person.java \
   wire-runtime/src/test/proto-java/com/squareup/wire/protos/person/Person.java.android

rm -r wire-runtime/src/test/proto-java.android

rm -r temp_gen_tests
