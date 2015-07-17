#!/bin/bash

set -e

mvn clean package -pl wire-compiler -am -Dmaven.test.skip
cd wire-runtime
pushd src/test/proto
PROTOS=`find . -name '*.proto' | sed 's/^\.\///'`
popd
echo $PROTOS
java -jar ../wire-compiler/target/wire-compiler-*-SNAPSHOT-jar-with-dependencies.jar \
  --proto_path=../wire-runtime/src/test/proto \
  --java_out=../wire-runtime/src/test/java \
  --enum_options=squareup.protos.custom_options.enum_value_option,squareup.protos.custom_options.complex_enum_value_option,squareup.protos.foreign.foreign_enum_value_option \
  ${PROTOS}

cp src/test/java/com/squareup/wire/protos/alltypes/* ../wire-gson-support/src/test/java/com/squareup/wire/protos/alltypes/
