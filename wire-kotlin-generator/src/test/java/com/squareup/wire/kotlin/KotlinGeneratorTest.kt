package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.schema.IdentifierSet
import com.squareup.wire.schema.RepoBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinGeneratorTest {
  @Test fun basic() {
    val repoBuilder = RepoBuilder()
      .add("message.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Person")
    assertTrue(code.contains("data class Person"))
    assertTrue(code.contains("object : ProtoAdapter<PhoneNumber>(\n"))
    assertTrue(code.contains("FieldEncoding.LENGTH_DELIMITED"))
    assertTrue(code.contains("PhoneNumber::class.java"))
    assertTrue(code.contains("override fun encode(writer: ProtoWriter, value: Person)"))
    assertTrue(code.contains("enum class PhoneType(private val value: Int) : WireEnum"))
    assertTrue(code.contains("WORK(1),"))
  }

  @Test fun defaultValues() {
    val repoBuilder = RepoBuilder()
      .add("message.proto", """
        |message Message {
        |  optional int32 a = 1 [default = 10 ];
        |  optional int32 b = 2 [default = 0x20 ];
        |  optional int64 c = 3 [default = 11 ];
        |  optional int64 d = 4 [default = 0x21 ];
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val a: Int = 10"))
    assertTrue(code.contains("val b: Int = 0x20"))
    assertTrue(code.contains("val c: Long = 11"))
    assertTrue(code.contains("val d: Long = 0x21"))
  }

  @Test fun nameAllocatorIsUsed() {
    val repoBuilder = RepoBuilder()
      .add("message.proto", """
        |message Message {
        |  required float when = 1;
        |  required int32 ADAPTER = 2;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val when_: Float"))
    assertTrue(code.contains("val ADAPTER_: Int"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.when_) +"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)"))
    assertTrue(code.contains("1 -> when_ = ProtoAdapter.FLOAT.decode(reader)"))
  }

  @Test fun enclosing() {
    val schema = RepoBuilder()
        .add("message.proto", """
          |message A {
          |  message B {
          |  }
          |  optional B b = 1;
          |}""".trimMargin())
        .schema()

    val pruned = schema.prune(IdentifierSet.Builder().include("A.B").build())

    val kotlinGenerator = KotlinGenerator.invoke(pruned)
    val typeSpec = kotlinGenerator.generateType(pruned.getType("A"))
    val code = FileSpec.get("", typeSpec).toString()
    assertTrue(code.contains("object A {"))
    assertTrue(code.contains("data class B(.*) : Message<B, B.Builder>".toRegex()))
  }
}
