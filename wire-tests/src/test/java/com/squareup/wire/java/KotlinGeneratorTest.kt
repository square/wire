package com.squareup.wire.java

import com.squareup.wire.schema.RepoBuilder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.IOException

class KotlinGeneratorTest {
  @Test fun test() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", ""
            + "message Person {\n" +
            "  required string name = 1;\n" +
            "  required int32 id = 2;\n" +
            "  optional string email = 3;\n" +
            "  enum PhoneType {\n" +
            "    vale = 0;\n" +
            "    value = 1;\n" +
            "    WORK = 2;\n" +
            "  }\n" +
            "  message PhoneNumber {\n" +
            "    required string number = 1;\n" +
            "    optional PhoneType type = 2 [default = HOME];\n" +
            "  }\n" +
            "  repeated PhoneNumber phone = 4;\n" +
            "}\n"
        )
    println(repoBuilder.generateKotlin("Person"))
  }

  @Test fun defaultValues() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  optional int32 a = 1 [default = 10 ];\n"
            + "  optional int32 b = 2 [default = 0x20 ];\n"
            + "  optional int64 c = 3 [default = 11 ];\n"
            + "  optional int64 d = 4 [default = 0x21 ];\n"
            + "}\n")
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val a: Int = 10"))
    assertTrue(code.contains("val b: Int = 0x20"))
    assertTrue(code.contains("val c: Long = 11"))
    assertTrue(code.contains("val d: Long = 0x21"))
  }

  @Test fun nameAllocatorIsUsed() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", ""
            + "message Message {\n"
            + "  required float when = 1;\n"
            + "  required int32 ADAPTER = 2;\n"
            + "}\n")
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val when_: Float"))
    assertTrue(code.contains("val ADAPTER_: Int"))
  }
}