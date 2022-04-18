package com.squareup.wire

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SchemaException
import kotlin.test.assertFailsWith
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SchemaBuilderTest {
  @Test fun emptySchema() {
    val exception = assertFailsWith<SchemaException> {
      buildSchema {}
    }
    assertThat(exception.message).isEqualTo("no sources")
  }

  @Test fun protoPathOnly() {
    val exception = assertFailsWith<SchemaException> {
      buildSchema {
        addToProtoPath(
          "example2.proto",
          """
          |syntax = "proto2";
          |
          |message D {
          |}
          |""".trimMargin()
        )
      }
    }
    assertThat(exception.message).isEqualTo("no sources")
  }

  @Test fun sourcePathOnly() {
    val schema = buildSchema {
      add(
        "example1.proto",
        """
          |syntax = "proto2";
          |
          |message A {
          |  optional B b = 1;
          |}
          |message B {
          |  optional C c = 1;
          |}
          |message C {
          |}
          |""".trimMargin()
      )
      add(
        "example2.proto",
        """
          |syntax = "proto2";
          |
          |message D {
          |}
          |""".trimMargin()
      )
    }
    assertThat(schema.protoFiles.map { it.location }).containsExactlyInAnyOrder(
      Location.get("/source", "example1.proto"),
      Location.get("/source", "example2.proto"),
      Location.get("google/protobuf/descriptor.proto"),
    )
  }

  @Test fun withProtoPath() {
    val schema =
      buildSchema(sourcePath = "/custom-source".toPath(), protoPath = "/custom-proto".toPath()) {
        add(
          "example1.proto",
          """
          |syntax = "proto2";
          |
          |import "example2.proto";
          |
          |message A {
          |  optional B b = 1;
          |}
          |message B {
          |  optional C c = 1;
          |}
          |""".trimMargin()
        )
        addToProtoPath(
          "example2.proto",
          """
          |syntax = "proto2";
          |
          |message C {
          |}
          |""".trimMargin()
        )
      }
    assertThat(schema.protoFiles.map { it.location }).containsExactlyInAnyOrder(
      Location.get("/custom-source", "example1.proto"),
      Location.get("/custom-proto", "example2.proto"),
      Location.get("google/protobuf/descriptor.proto"),
    )
  }
}
