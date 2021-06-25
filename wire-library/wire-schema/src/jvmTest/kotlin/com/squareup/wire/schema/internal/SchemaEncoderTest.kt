package com.squareup.wire.schema.internal

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.MethodOptions
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.UnknownFieldSet
import com.squareup.wire.schema.RepoBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SchemaEncoderTest {
  @Test fun `encode schema`() {
    val schema = RepoBuilder()
      .add(
        "handle_service.proto", """
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
            |""".trimMargin()
      )
      .schema()

    val handleServiceProto = schema.protoFile("handle_service.proto")!!
    val encoded = SchemaEncoder(schema).encode(handleServiceProto)

    val fileDescriptorProto = FileDescriptorProto.parseFrom(encoded.toByteArray())
    assertThat(fileDescriptorProto).isEqualTo(FileDescriptorProto.newBuilder()
      .setName("handle_service.proto")
      .addDependency("google/protobuf/descriptor.proto")
      .addEnumType(EnumDescriptorProto.newBuilder()
        .setName("GreekLetter")
        .addValue(EnumValueDescriptorProto.newBuilder()
          .setName("ALPHA")
          .setNumber(1)
          .build())
        .addValue(EnumValueDescriptorProto.newBuilder()
          .setName("BETA")
          .setNumber(2)
          .build())
        .build())
      .addMessageType(DescriptorProto.newBuilder()
        .setName("HandleRequest")
        .build())
      .addMessageType(DescriptorProto.newBuilder()
        .setName("HandleResponse")
        .build())
      .addService(ServiceDescriptorProto.newBuilder()
        .setName("HandleService")
        .addMethod(MethodDescriptorProto.newBuilder()
          .setName("Handle")
          .setInputType(".HandleRequest")
          .setOutputType(".HandleResponse")
          .setOptions(MethodOptions.newBuilder()
            .setUnknownFields(UnknownFieldSet.newBuilder()
              .addField(22000, UnknownFieldSet.Field.newBuilder()
                .addFixed64(java.lang.Double.doubleToLongBits(2.1))
                .build())
              .addField(22001, UnknownFieldSet.Field.newBuilder()
                .addVarint(2L)
                .build())
              .addField(22002, UnknownFieldSet.Field.newBuilder()
                .addVarint(1L)
                .addVarint(2L)
                .addVarint(1L)
                .build())
              .build())
          .build())
        .build()))
      .addExtension(FieldDescriptorProto.newBuilder()
        .setName("timeout")
        .setExtendee(".google.protobuf.MethodOptions")
        .setNumber(22000)
        .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
        .setType(FieldDescriptorProto.Type.TYPE_DOUBLE)
        .build())
      .addExtension(FieldDescriptorProto.newBuilder()
        .setName("greek_letter")
        .setExtendee(".google.protobuf.MethodOptions")
        .setNumber(22001)
        .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
        .setType(FieldDescriptorProto.Type.TYPE_ENUM)
        .setTypeName(".GreekLetter")
        .build())
      .addExtension(FieldDescriptorProto.newBuilder()
        .setName("fraternity")
        .setExtendee(".google.protobuf.MethodOptions")
        .setNumber(22002)
        .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
        .setType(FieldDescriptorProto.Type.TYPE_ENUM)
        .setTypeName(".GreekLetter")
        .build())
      .build())
  }
}
