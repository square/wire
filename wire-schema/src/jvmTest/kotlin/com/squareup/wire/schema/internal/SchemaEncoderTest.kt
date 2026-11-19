/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema.internal

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.DescriptorProto.ExtensionRange
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.UnknownFieldSet
import com.squareup.wire.buildSchema
import com.squareup.wire.schema.MessageType
import kotlin.test.Test
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath

class SchemaEncoderTest {
  @Test fun `encode schema`() {
    val schema = buildSchema {
      add(
        "handle_service.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |import "google/protobuf/descriptor.proto";
        |
        |enum GreekLetter {
        |  ALPHA = 1;
        |  BETA = 2;
        |}
        |
        |extend google.protobuf.MethodOptions {
        |  optional double timeout = 22000;
        |  optional GreekLetter greek_letter = 22001;
        |  repeated GreekLetter fraternity = 22002;
        |}
        |
        |message HandleRequest {
        |}
        |
        |message HandleResponse {
        |}
        |
        |service HandleService {
        |  rpc Handle ( HandleRequest ) returns ( HandleResponse ) {
        |    option (timeout) = 2.1;
        |    option (greek_letter) = BETA;
        |    option (fraternity) = [ALPHA, BETA, ALPHA];
        |  }
        |}
        |
        """.trimMargin(),
      )
    }

    val handleServiceProto = schema.protoFile("handle_service.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isEqualTo(
      FileDescriptorProto.newBuilder()
        .setName("handle_service.proto")
        .addDependency("google/protobuf/descriptor.proto")
        .addEnumType(
          EnumDescriptorProto.newBuilder()
            .setName("GreekLetter")
            .addValue(
              EnumValueDescriptorProto.newBuilder()
                .setName("ALPHA")
                .setNumber(1)
                .build(),
            )
            .addValue(
              EnumValueDescriptorProto.newBuilder()
                .setName("BETA")
                .setNumber(2)
                .build(),
            )
            .build(),
        )
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("HandleRequest")
            .build(),
        )
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("HandleResponse")
            .build(),
        )
        .addService(
          ServiceDescriptorProto.newBuilder()
            .setName("HandleService")
            .addMethod(
              MethodDescriptorProto.newBuilder()
                .setName("Handle")
                .setInputType(".HandleRequest")
                .setOutputType(".HandleResponse")
                .setOptions(
                  MethodOptions.newBuilder()
                    .setUnknownFields(
                      UnknownFieldSet.newBuilder()
                        .addField(
                          22000,
                          UnknownFieldSet.Field.newBuilder()
                            .addFixed64(java.lang.Double.doubleToLongBits(2.1))
                            .build(),
                        )
                        .addField(
                          22001,
                          UnknownFieldSet.Field.newBuilder()
                            .addVarint(2L)
                            .build(),
                        )
                        .addField(
                          22002,
                          UnknownFieldSet.Field.newBuilder()
                            .addVarint(1L)
                            .addVarint(2L)
                            .addVarint(1L)
                            .build(),
                        )
                        .build(),
                    )
                    .build(),
                )
                .build(),
            ),
        )
        .addExtension(
          FieldDescriptorProto.newBuilder()
            .setName("timeout")
            .setExtendee(".google.protobuf.MethodOptions")
            .setNumber(22000)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_DOUBLE)
            .build(),
        )
        .addExtension(
          FieldDescriptorProto.newBuilder()
            .setName("greek_letter")
            .setExtendee(".google.protobuf.MethodOptions")
            .setNumber(22001)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_ENUM)
            .setTypeName(".GreekLetter")
            .build(),
        )
        .addExtension(
          FieldDescriptorProto.newBuilder()
            .setName("fraternity")
            .setExtendee(".google.protobuf.MethodOptions")
            .setNumber(22002)
            .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
            .setType(FieldDescriptorProto.Type.TYPE_ENUM)
            .setTypeName(".GreekLetter")
            .build(),
        )
        .build(),
    )
  }

  @Test fun `encode packed option`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  repeated int32 sizes = 22003 [packed = true];
        |}
        |
        |message TestMessage {
        |  option (sizes) = [1, 2, 300];
        |}
        |
        """.trimMargin(),
      )
    }

    val encoded = SchemaEncoder(schema).encode(schema.protoFile("test.proto")!!)

    // protoc writes one length-delimited record: tag 22003, length 4, then 1, 2, and 300.
    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    val options = fileDescriptorProto.getMessageType(0).options.toByteString()
    assertThat(options.toByteArray().toByteString().hex()).isEqualTo("9adf0a040102ac02")
  }

  @Test fun `encode extension range`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message TestMessage {
        |  extensions 5, 1000 to max;
        |}
        |
        """.trimMargin(),
      )
    }

    val handleServiceProto = schema.protoFile("test.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isEqualTo(
      FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addExtensionRange(
              ExtensionRange.newBuilder()
                .setStart(5)
                .setEnd(6)
                .build(),
            )
            .addExtensionRange(
              ExtensionRange.newBuilder()
                .setStart(1000)
                .setEnd(MAX_TAG_VALUE + 1)
                .build(),
            )
            .build(),
        )
        .build(),
    )
  }

  @Test fun `encode nested enums`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message TestMessage {
            |  enum Nested {
            |    NESTED_UNDEFINED = 0;
            |    NESTED_DEFINED = 1;
            |  }
            |}
            |
        """.trimMargin(),
      )
    }

    val handleServiceProto = schema.protoFile("test.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isEqualTo(
      FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addEnumType(
              EnumDescriptorProto.newBuilder()
                .setName("Nested")
                .addValue(0, EnumValueDescriptorProto.newBuilder().setName("NESTED_UNDEFINED").setNumber(0))
                .addValue(1, EnumValueDescriptorProto.newBuilder().setName("NESTED_DEFINED").setNumber(1)),
            )
            .build(),
        )
        .build(),
    )
  }

  @Test fun `oneof tag order`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |message AMessage {
        |  string two = 2;
        |
        |  oneof a_oneof {
        |    string one = 1;
        |    string three = 3;
        |  }
        |}
        |
        """.trimMargin(),
      )
    }
    val handleServiceProto = schema.protoFile("test.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isNotNull()
    assertThat(fileDescriptorProto).isEqualTo(
      FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .setSyntax("proto3")
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("AMessage")
            .addField(
              FieldDescriptorProto.newBuilder()
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setName("two")
                .setNumber(2)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .build(),
            )
            .addField(
              FieldDescriptorProto.newBuilder()
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setName("one")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .setOneofIndex(0)
                .build(),
            )
            .addField(
              FieldDescriptorProto.newBuilder()
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setName("three")
                .setNumber(3)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .setOneofIndex(0)
                .build(),
            )
            .addOneofDecl(
              DescriptorProtos.OneofDescriptorProto.newBuilder()
                .setName("a_oneof")
                .build(),
            )
            .build(),
        )
        .build(),
    )
  }

  @Test fun `encode import public dependency`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |import "standard.proto";
            |import public "public.proto";
            |
            |message TestMessage {
            |  optional StandardMessage standard_message = 1;
            |  optional PublicMessage public_message = 2;
            |}
            |
        """.trimMargin(),
      )
      add(
        "standard.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message StandardMessage {
            |  optional string value = 1;
            |}
            |
        """.trimMargin(),
      )
      add(
        "public.proto".toPath(),
        """
            |syntax = "proto2";
            |
            |message PublicMessage {
            |  optional string value = 1;
            |}
            |
        """.trimMargin(),
      )
    }

    val handleServiceProto = schema.protoFile("test.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isEqualTo(
      FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .addDependency("standard.proto")
        .addDependency("public.proto")
        .addPublicDependency(1)
        .addMessageType(
          DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(
              FieldDescriptorProto.newBuilder()
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setName("standard_message")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .setTypeName(".StandardMessage")
                .build(),
            )
            .addField(
              FieldDescriptorProto.newBuilder()
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setName("public_message")
                .setNumber(2)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
                .setTypeName(".PublicMessage")
                .build(),
            )
            .build(),
        )
        .build(),
    )
  }

  @Test fun `encode bytes defaults one char per byte`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message TestMessage {
        |  optional bytes ascii = 1 [default = "abc"];
        |  optional bytes octal = 2 [default = "a\351\377z"];
        |  optional bytes hex = 3 [default = "\xe9\xFF"];
        |  optional bytes special = 4 [default = "\n\t\"\'\\\000~\177"];
        |  optional string text = 5 [default = "caf\351"];
        |}
        |
        """.trimMargin(),
      )
    }

    val encoded = SchemaEncoder(schema).encode(schema.protoFile("test.proto")!!)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    val defaults = fileDescriptorProto.getMessageType(0).fieldList.associate { it.name to it.defaultValue }
    // The descriptor holds the C-escaped text of the bytes, like protoc writes it.
    assertThat(defaults).isEqualTo(
      mapOf(
        "ascii" to "abc",
        "octal" to "a\\351\\377z",
        "hex" to "\\351\\377",
        "special" to "\\n\\t\\\"\\'\\\\\\000~\\177",
        "text" to "caf\u00e9",
      ),
    )

    // A descriptor consumer reads one byte for each char of the original default.
    val file = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, emptyArray())
    val fields = file.findMessageTypeByName("TestMessage").fields.associateBy { it.name }
    assertThat((fields.getValue("ascii").defaultValue as ByteString).toByteArray().toByteString().hex())
      .isEqualTo("616263")
    assertThat((fields.getValue("octal").defaultValue as ByteString).toByteArray().toByteString().hex())
      .isEqualTo("61e9ff7a")
    assertThat((fields.getValue("hex").defaultValue as ByteString).toByteArray().toByteString().hex())
      .isEqualTo("e9ff")
    assertThat((fields.getValue("special").defaultValue as ByteString).toByteArray().toByteString().hex())
      .isEqualTo("0a0922275c007e7f")
  }

  @Test fun `encode bytes option one char per byte`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  optional bytes raw = 22005;
        |}
        |
        |message TestMessage {
        |  option (raw) = "a\351\377";
        |}
        |
        """.trimMargin(),
      )
    }

    val encoded = SchemaEncoder(schema).encode(schema.protoFile("test.proto")!!)

    // The record is tag 22005, length 3, then the bytes 0x61, 0xe9, and 0xff.
    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    val options = fileDescriptorProto.getMessageType(0).options.toByteString()
    assertThat(options.toByteArray().toByteString().hex()).isEqualTo("aadf0a0361e9ff")
  }

  @Test fun `encode bytes default above U+00FF as question mark`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message TestMessage {
        |  optional bytes big = 1 [default = "\400"];
        |}
        |
        """.trimMargin(),
      )
    }

    val encoded = SchemaEncoder(schema).encode(schema.protoFile("test.proto")!!)

    // Generated code turns a char above U+00FF into a question mark, and so does the descriptor.
    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto.getMessageType(0).getField(0).defaultValue).isEqualTo("?")
  }

  @Test fun `encode bytes option above U+00FF as question mark`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |import "google/protobuf/descriptor.proto";
        |
        |extend google.protobuf.MessageOptions {
        |  optional bytes raw = 22005;
        |}
        |
        |message TestMessage {
        |  option (raw) = "\400";
        |}
        |
        """.trimMargin(),
      )
    }

    val encoded = SchemaEncoder(schema).encode(schema.protoFile("test.proto")!!)

    // The record is tag 22005, length 1, then a question mark.
    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    val options = fileDescriptorProto.getMessageType(0).options.toByteString()
    assertThat(options.toByteArray().toByteString().hex()).isEqualTo("aadf0a013f")
  }

  @Test fun `proto output and descriptor name the same bytes for a bytes default`() {
    val schema = buildSchema {
      add(
        "test.proto".toPath(),
        """
        |syntax = "proto2";
        |
        |message TestMessage {
        |  optional bytes octal = 1 [default = "\351"];
        |  optional bytes raw = 2 [default = "é"];
        |  optional bytes big = 3 [default = "\400"];
        |  optional bytes mixed = 4 [default = "a\351\400z"];
        |}
        |
        """.trimMargin(),
      )
    }
    val protoFile = schema.protoFile("test.proto")!!

    // The .proto output writes one octal escape for each of those chars, and a question mark above U+00FF.
    val protoOutput = protoFile.toSchema()
    assertThat(protoOutput).contains("optional bytes octal = 1 [default = \"\\351\"];")
    assertThat(protoOutput).contains("optional bytes raw = 2 [default = \"\\351\"];")
    assertThat(protoOutput).contains("optional bytes big = 3 [default = \"?\"];")
    assertThat(protoOutput).contains("optional bytes mixed = 4 [default = \"a\\351?z\"];")

    // The descriptor names the same bytes. A consumer reads one byte for each char.
    val fileDescriptorProto = FileDescriptorProto.parseFrom(SchemaEncoder(schema).encode(protoFile).toByteArray())
    val descriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, emptyArray())
    val fields = descriptor.findMessageTypeByName("TestMessage").fields.associateBy { it.name }
    val descriptorBytes = fields.mapValues { (it.value.defaultValue as ByteString).toByteArray().toByteString().hex() }
    assertThat(descriptorBytes).isEqualTo(
      mapOf("octal" to "e9", "raw" to "e9", "big" to "3f", "mixed" to "61e93f7a"),
    )

    // Reading the .proto output back gives the same bytes as the descriptor.
    val reparsed = buildSchema { add("test.proto".toPath(), protoOutput) }
    val reparsedBytes = (reparsed.getType("TestMessage") as MessageType)
      .fields.associate { it.name to it.default!!.toBytes().hex() }
    assertThat(reparsedBytes).isEqualTo(descriptorBytes)
  }
}
