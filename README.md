"Wire" Mobile Protocol Buffers
==============================

*“A man got to have a code!”* - Omar Little


Introduction
------------

Wire is a library for lightweight protocol buffers for mobile Java. Code generated by Wire has many
fewer methods than standard protocol buffer code, which helps applications avoid the notorious 64k
limit on methods in Android applications. Wire also generates clean, human-readable code for
protocol buffer messages.


Compiling .proto files
----------------------

The 'wire-compiler' package contains the `WireCompiler` class, which compiles standard .proto files
into Java.

For example, to compile the file `protos-repo/google/protobuf/descriptor.proto`, which may
(recursively) import other .proto files within the `protos-repo/` directory:

    % mvn clean package

    % java -jar compiler/target/wire-compiler-0.2-SNAPSHOT.jar --proto_path=protos-repo \
        --java_out=out google/protobuf/descriptor.proto
    Reading proto source file protos-repo/google/protobuf/descriptor.proto
    Writing generated code to out/com/google/protobuf/DescriptorProtos.java

    # The output has been written to out/com/google/protobuf/DescriptorProtos.java
    % head -18 out/com/google/protobuf/DescriptorProtos.java
    /**
     * Code generated by Square Wire protobuf compiler, do not edit.
     * Source file: protos-repo/google/protobuf/descriptor.proto
     */
    package com.google.protobuf;

    import com.squareup.wire.Message;
    import com.squareup.wire.Wire;
    import com.squareup.wire.ProtoEnum;
    import com.squareup.wire.ProtoField;
    import com.squareup.wire.UninitializedMessageException;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;
    import java.util.TreeMap;

    public final class DescriptorProtos {

Instead of supplying individual filename arguments on the command line, the `--files` flag may be
used to specify a single file containing a list of .proto files. The file names are interpreted
relative to the value given for the `--proto_path` flag.

    % cat protos.include
    google/protobuf/descriptor.proto
    yourcompany/protos/stuff.proto
    ...

    % java -jar compiler/target/wire-compiler-0.2-SNAPSHOT.jar --proto_path=protos-repo --java_out=out --files=protos.include
    Reading proto source file protos-repo/google/protobuf/descriptor.proto
    Writing generated code to out/com/google/protobuf/DescriptorProtos.java
    Reading proto source file protos-repo/yourcompany/protos/stuff.proto
    Writing generated code to out/com/yourcompany/protos/stuff/Stuff.java
    ...


Using Wire in your application
------------------------------

The 'wire-runtime' package contains runtime support libraries that must be included in applications
that use Wire-generated code.

The code in `com.google.protobuf.nano` is taken from the Android Open Source repo, with
modifications.

For Maven projects, simply add wire-runtime as a dependency:

```xml
<dependency>
  <groupId>com.squareup.wire</groupId>
  <artifactId>wire-runtime</artifactId>
  <version>0.2-SNAPSHOT</version>
</dependency>
```


Future work
-----------

Some things that aren't implemented:

 * Groups
 * Immutable byte array wrappers
 * Unknown fields
 * Services
