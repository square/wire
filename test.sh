WIREHOME=~/workspace/wire
java  -jar $WIREHOME/wire-compiler/target/wire-compiler-2.0.0-SNAPSHOT-jar-with-dependencies.jar --proto_path=$WIREHOME/protos-repo --java_out=$WIREHOME/out  beeshop_cmd.proto
