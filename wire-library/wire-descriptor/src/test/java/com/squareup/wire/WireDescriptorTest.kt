package com.squareup.wire

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor
import okio.Path.Companion.toPath
import org.junit.Test

class WireDescriptorTest {
  @Test fun schemaToFileDescriptorSet() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Person {
        |	required string name = 1;
        |	required int32 id = 2;
        |	optional string email = 3;
        |	enum PhoneType {
        |		HOME = 0;
        |		WORK = 1;
        |		MOBILE = 2;
        |	}
        |	message PhoneNumber {
        |		required string number = 1;
        |		optional PhoneType type = 2 [default = HOME];
        |	}
        |	repeated PhoneNumber phone = 4;
        |}""".trimMargin()
      )
    }

    val fileDescriptorSet = WireDescriptor(schema).fileDescriptorSet()
    val descriptorProto = FileDescriptor.buildFrom(fileDescriptorSet.getFile(0), emptyArray())
    assertThat(descriptorProto.name).isEqualTo("google/protobuf/descriptor.proto")

    val phoneTypeEnum = EnumDescriptorProto.newBuilder()
      .setName("PhoneType")
      .addValue(EnumValueDescriptorProto.newBuilder().setName("HOME").setNumber(0).build())
      .addValue(EnumValueDescriptorProto.newBuilder().setName("WORK").setNumber(1).build())
      .addValue(EnumValueDescriptorProto.newBuilder().setName("MOBILE").setNumber(2).build())
      .build()
    val phoneNumber = DescriptorProto.newBuilder()
      .setName("PhoneNumber")
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_REQUIRED)
          .setType(TYPE_STRING)
          .setName("number")
          .setNumber(1)
          .build()
      )
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_OPTIONAL)
          .setType(TYPE_ENUM)
          .setTypeName(".Person.PhoneType")
          .setName("type")
          .setNumber(2)
          .setDefaultValue("HOME")
          .build()
      )
      .build()
    val person = DescriptorProto.newBuilder()
      .setName("Person")
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_REQUIRED)
          .setType(TYPE_STRING)
          .setName("name")
          .setNumber(1)
          .build()
      )
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_REQUIRED)
          .setType(TYPE_INT32)
          .setName("id")
          .setNumber(2)
          .build()
      )
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_OPTIONAL)
          .setType(TYPE_STRING)
          .setName("email")
          .setNumber(3)
          .build()
      )
      .addField(
        FieldDescriptorProto.newBuilder()
          .setLabel(LABEL_REPEATED)
          .setType(TYPE_MESSAGE)
          .setTypeName(".Person.PhoneNumber")
          .setName("phone")
          .setNumber(4)
          .build()
      )
      .addNestedType(phoneNumber)
      .addEnumType(phoneTypeEnum)
      .build()
    val expected = FileDescriptorProto.newBuilder()
      .setName("message.proto")
      .addMessageType(person)
      .build()

    val messageProto = fileDescriptorSet.getFile(1)
    assertThat(messageProto).isEqualTo(expected)
  }
}
