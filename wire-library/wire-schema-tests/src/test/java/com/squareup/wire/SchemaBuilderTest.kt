package com.squareup.wire

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SchemaException
import kotlin.test.assertFailsWith
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SchemaBuilderTest {
  @Test fun emptySchema() {
    val exception = assertFailsWith<SchemaException> {
      buildSchema {}
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
}
