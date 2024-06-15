/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire

import com.google.protobuf.Duration
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.junit.Ignore
import org.junit.Test
import squareup.proto2.java.interop.InteropBoxOneOf as InteropBoxOneOfJ2
import squareup.proto2.java.interop.InteropCamelCase as InteropCamelCaseJ2
import squareup.proto2.java.interop.InteropDuration as InteropDurationJ2
import squareup.proto2.java.interop.InteropJsonName as InteropJsonNameJ2
import squareup.proto2.java.interop.InteropTest.InteropCamelCase as InteropCamelCaseP2
import squareup.proto2.java.interop.InteropTest.InteropJsonName as InteropJsonNameP2
import squareup.proto2.java.interop.InteropTest.InteropUint64 as InteropUint64P2
import squareup.proto2.java.interop.InteropUint64 as InteropUint64J2
import squareup.proto2.kotlin.buildersonly.BuildersOnlyMessage as BuildersOnlyMessageK2
import squareup.proto2.kotlin.buildersonly.BuildersOnlyMessage.Companion.PACKAGE_VALUE
import squareup.proto2.kotlin.buildersonly.BuildersOnlyMessage.NestedMessage as NestedMessageK2
import squareup.proto2.kotlin.buildersonly.BuildersOnlyMessageOuterClass.BuildersOnlyMessage as BuildersOnlyMessageP2
import squareup.proto2.kotlin.buildersonly.BuildersOnlyMessageOuterClass.BuildersOnlyMessage.NestedMessage as NestedMessageP2
import squareup.proto2.kotlin.interop.InteropBoxOneOf as InteropBoxOneOfK2
import squareup.proto2.kotlin.interop.InteropCamelCase as InteropCamelCaseK2
import squareup.proto2.kotlin.interop.InteropDuration as InteropDurationK2
import squareup.proto2.kotlin.interop.InteropJsonName as InteropJsonNameK2
import squareup.proto2.kotlin.interop.InteropUint64 as InteropUint64K2
import squareup.proto3.java.interop.InteropBoxOneOf as InteropBoxOneOfJ3
import squareup.proto3.java.interop.InteropCamelCase as InteropCamelCaseJ3
import squareup.proto3.java.interop.InteropDuration as InteropDurationJ3
import squareup.proto3.java.interop.InteropJsonName as InteropJsonNameJ3
import squareup.proto3.java.interop.InteropOptional as InteropOptionalJ3
import squareup.proto3.java.interop.InteropTest.InteropBoxOneOf as InteropBoxOneOfP3
import squareup.proto3.java.interop.InteropTest.InteropCamelCase as InteropCamelCaseP3
import squareup.proto3.java.interop.InteropTest.InteropDuration as InteropDurationP3
import squareup.proto3.java.interop.InteropTest.InteropJsonName as InteropJsonNameP3
import squareup.proto3.java.interop.InteropTest.InteropUint64 as InteropUint64P3
import squareup.proto3.java.interop.InteropTest.InteropWrappers
import squareup.proto3.java.interop.InteropUint64 as InteropUint64J3
import squareup.proto3.java.interop.InteropWrappers as InteropWrappersJ3
import squareup.proto3.kotlin.interop.InteropBoxOneOf as InteropBoxOneOfK3
import squareup.proto3.kotlin.interop.InteropCamelCase as InteropCamelCaseK3
import squareup.proto3.kotlin.interop.InteropDuration as InteropDurationK3
import squareup.proto3.kotlin.interop.InteropJsonName as InteropJsonNameK3
import squareup.proto3.kotlin.interop.InteropOptional as InteropOptionalK3
import squareup.proto3.kotlin.interop.InteropUint64 as InteropUint64K3
import squareup.proto3.kotlin.interop.InteropWrappers as InteropWrappersK3
import squareup.proto3.kotlin.interop.TestProto3Optional.InteropOptional as InteropOptionalP3
import squareup.proto3.kotlin.unrecognized_constant.Easter as EasterK3
import squareup.proto3.kotlin.unrecognized_constant.EasterOuterClass.Easter as EasterP3

class InteropTest {
  @Test fun duration() {
    val checker = InteropChecker(
      protocMessage = InteropDurationP3.newBuilder()
        .setValue(
          Duration.newBuilder()
            .setSeconds(99L)
            .setNanos(987_654_321)
            .build(),
        )
        .build(),
      canonicalJson = """{"value":"99.987654321s"}""",
      wireAlternateJsons = listOf(
        // TODO: move to alternateJsons once we can use ignoringUnknownFields().
        """{"unused": false, "value":"99.987654321s"}""",
      ),
    )

    checker.check(InteropDurationK3(durationOfSeconds(99, 987_654_321L)))
    checker.check(InteropDurationJ3(durationOfSeconds(99, 987_654_321L)))
    checker.check(InteropDurationK2(durationOfSeconds(99, 987_654_321L)))
    checker.check(InteropDurationJ2(durationOfSeconds(99, 987_654_321L)))
  }

  @Test fun uint64() {
    val zero = InteropChecker(
      protocMessage = InteropUint64P3.newBuilder()
        .setValue(0L)
        .build(),
      canonicalJson = """{}""",
      alternateJsons = listOf(
        """{"value":"0"}""",
        """{"value":0}""",
        """{"value":"-0"}""",
      ),
    )
    zero.check(InteropUint64K3(0L))
    zero.check(InteropUint64J3(0L))

    val one = InteropChecker(
      protocMessage = InteropUint64P3.newBuilder()
        .setValue(1L)
        .build(),
      canonicalJson = """{"value":"1"}""",
      alternateJsons = listOf(
        """{"value":1}""",
        """{"value":"1"}""",
        """{"value":"1.0"}""",
      ),
    )
    one.check(InteropUint64K3(1L))
    one.check(InteropUint64J3(1L))

    val max = InteropChecker(
      protocMessage = InteropUint64P3.newBuilder()
        .setValue(-1L)
        .build(),
      canonicalJson = """{"value":"18446744073709551615"}""",
      wireAlternateJsons = listOf(
        """{"value":"-1"}""",
      ),
    )
    max.check(InteropUint64K3(-1L))
    max.check(InteropUint64J3(-1L))
  }

  @Test fun `uint64 proto 2`() {
    val max = InteropChecker(
      protocMessage = InteropUint64P2.newBuilder()
        .setValue(-1L)
        .build(),
      canonicalJson = """{"value":"18446744073709551615"}""",
      wireCanonicalJson = """{"value":18446744073709551615}""",
      alternateJsons = listOf(
        """{"value":"18446744073709551615"}""",
        """{"value":18446744073709551615}""",
      ),
      wireAlternateJsons = listOf(
        """{"value":"-1"}""",
      ),
    )
    max.check(InteropUint64K2(-1L))
    max.check(InteropUint64J2(-1L))
  }

  @Test fun `camel case`() {
    val checker = InteropChecker(
      protocMessage = InteropCamelCaseP3.newBuilder()
        .setHelloWorld("1")
        .setAB("2")
        .setCccDdd("3")
        .setEEeeFfGGg("4")
        .setABC("5")
        .setGHI("6")
        .setKLM("7")
        .setTUV("8")
        .setXYZ("9")
        .build(),
      canonicalJson = """{"helloWorld":"1","aB":"2","CccDdd":"3","EEeeFfGGg":"4","aBC":"5","GHI":"6","KLM":"7","TUV":"8","XYZ":"9"}""",
      alternateJsons = listOf(
        """{"hello_world": "1", "a__b": "2", "_Ccc_ddd": "3", "EEee_ff_gGg": "4", "a_b_c": "5", "GHI": "6", "K_L_M": "7", "__T__U__V__": "8", "_x_y_z_": "9"}""",
      ),
    )

    checker.check(InteropCamelCaseK3("1", "2", "3", "4", "5", "6", "7", "8", "9"))
    checker.check(InteropCamelCaseJ3("1", "2", "3", "4", "5", "6", "7", "8", "9"))
  }

  @Test fun `camel case proto 2`() {
    val checker = InteropChecker(
      protocMessage = InteropCamelCaseP2.newBuilder()
        .setHelloWorld("1")
        .setAB("2")
        .setCccDdd("3")
        .setEEeeFfGGg("4")
        .setABC("5")
        .setGHI("6")
        .setKLM("7")
        .setTUV("8")
        .setXYZ("9")
        .build(),
      canonicalJson = """{"helloWorld":"1","aB":"2","CccDdd":"3","EEeeFfGGg":"4","aBC":"5","GHI":"6","KLM":"7","TUV":"8","XYZ":"9"}""",
      wireCanonicalJson = """{"hello_world":"1","a__b":"2","_Ccc_ddd":"3","EEee_ff_gGg":"4","a_b_c":"5","GHI":"6","K_L_M":"7","__T__U__V__":"8","_x_y_z_":"9"}""",
      alternateJsons = listOf(
        """{"helloWorld":"1","aB":"2","CccDdd":"3","EEeeFfGGg":"4","aBC":"5","GHI":"6","KLM":"7","TUV":"8","XYZ":"9"}""",
      ),
    )

    checker.check(InteropCamelCaseK2("1", "2", "3", "4", "5", "6", "7", "8", "9"))
    checker.check(InteropCamelCaseJ2("1", "2", "3", "4", "5", "6", "7", "8", "9"))
  }

  @Test fun `json names`() {
    val checker = InteropChecker(
      protocMessage = InteropJsonNameP3.newBuilder()
        .setA("1")
        .setPublic("2")
        .setCamelCase("3")
        .build(),
      canonicalJson = """{"one":"1","two":"2","three":"3"}""",
      alternateJsons = listOf(
        """{"a":"1","public":"2","camel_case":"3"}""",
      ),
    )

    checker.check(InteropJsonNameJ3("1", "2", "3"))
    checker.check(InteropJsonNameK3("1", "2", "3"))
  }

  @Test fun `json names proto2`() {
    val checker = InteropChecker(
      protocMessage = InteropJsonNameP2.newBuilder()
        .setA("1")
        .setPublic("2")
        .setCamelCase("3")
        .build(),
      canonicalJson = """{"one":"1","two":"2","three":"3"}""",
      alternateJsons = listOf(
        """{"a":"1","public":"2","camel_case":"3"}""",
      ),
    )

    checker.check(InteropJsonNameJ2("1", "2", "3"))
    checker.check(InteropJsonNameK2("1", "2", "3"))
  }

  @Test fun optionalNonIdentity() {
    val checker = InteropChecker(
      protocMessage = InteropOptionalP3.newBuilder()
        .setValue("hello")
        .build(),
      canonicalJson = """{"value":"hello"}""",
      wireAlternateJsons = listOf(
        """{"unused": false, "value":"hello"}""",
      ),
    )

    checker.check(InteropOptionalK3("hello"))
    checker.check(InteropOptionalJ3("hello"))
  }

  @Test fun optionalIdentity() {
    val checker = InteropChecker(
      protocMessage = InteropOptionalP3.newBuilder()
        .setValue("")
        .build(),
      canonicalJson = """{"value":""}""",
      wireAlternateJsons = listOf(
        """{"unused": false, "value":""}""",
      ),
    )

    checker.check(InteropOptionalK3(""))
    checker.check(InteropOptionalJ3(""))
  }

  @Test fun boxOneOfsKotlin() {
    val checker = InteropChecker(
      protocMessage = InteropBoxOneOfP3.newBuilder()
        .setA("Hello")
        .build(),
      canonicalJson = """{"a":"Hello"}""",
    )
    checker.check(
      InteropBoxOneOfK2.Builder()
        .option(OneOf(InteropBoxOneOfK2.OPTION_A, "Hello"))
        .build(),
    )
    checker.check(
      InteropBoxOneOfK3.Builder()
        .option(OneOf(InteropBoxOneOfK3.OPTION_A, "Hello"))
        .build(),
    )
  }

  @Ignore("Needs to implement boxed oneofs in Java.")
  @Test
  fun boxOneOfsJava() {
    val checker = InteropChecker(
      protocMessage = InteropBoxOneOfP3.newBuilder()
        .setA("Hello")
        .build(),
      canonicalJson = """{"a":"Hello"}""",
    )
    checker.check(InteropBoxOneOfJ2.Builder().a("Hello").build())
    checker.check(InteropBoxOneOfJ3.Builder().a("Hello").build())
  }

  @Test fun wrappersDoesNotOmitWrappedIdentityValues() {
    val checker = InteropChecker(
      protocMessage = InteropWrappers.newBuilder()
        .setDoubleValue(0.0.toDoubleValue())
        .setFloatValue(0f.toFloatValue())
        .setInt64Value(0L.toInt64Value())
        .setUint64Value(0L.toUInt64Value())
        .setInt32Value(0.toInt32Value())
        .setUint32Value(0.toUInt32Value())
        .setBoolValue(false.toBoolValue())
        .setStringValue("".toStringValue())
        .setBytesValue(ByteString.EMPTY.toBytesValue())
        .build(),
      canonicalJson = """{"doubleValue":0.0,"floatValue":0.0,"int64Value":"0","uint64Value":"0","int32Value":0,"uint32Value":0,"boolValue":false,"stringValue":"","bytesValue":""}""",
    )
    checker.check(
      InteropWrappersJ3.Builder()
        .double_value(0.0)
        .float_value(0f)
        .int64_value(0L)
        .uint64_value(0L)
        .int32_value(0)
        .uint32_value(0)
        .bool_value(false)
        .string_value("")
        .bytes_value(ByteString.EMPTY)
        .build(),
    )
    checker.check(
      InteropWrappersK3.Builder()
        .double_value(0.0)
        .float_value(0f)
        .int64_value(0L)
        .uint64_value(0L)
        .int32_value(0)
        .uint32_value(0)
        .bool_value(false)
        .string_value("")
        .bytes_value(ByteString.EMPTY)
        .build(),
    )
  }

  @Test fun wrappersWithNulls() {
    val checker = InteropChecker(
      protocMessage = InteropWrappers.newBuilder().build(),
      canonicalJson = """{}""",
    )
    checker.check(InteropWrappersJ3.Builder().build())
    checker.check(InteropWrappersK3.Builder().build())
  }

  @Test fun keywordNamedConstant() {
    // ┌─ 2: 15
    // ├─ 3: 2
    // ├─ 4: 1
    // ├─ 4: 15
    // ╰- 5: 15
    val bytes = "100f18022001200f2a010f"
    val wireMessage: EasterK3 = EasterK3.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP3 = EasterP3.parseFrom(bytes.decodeHex().toByteArray())

    val checker = InteropChecker(
      protocMessage = protocMessage,
      canonicalJson = """{"optionalEasterAnimal":"object","identityEasterAnimal":"HEN","easterAnimalsRepeated":["BUNNY","object"],"easterAnimalsPacked":["object"]}""",
    )

    checker.check(wireMessage)
  }

  @Test fun unknownEnumsWithUnrecognizedConstant() {
    // ┌─ 2: 5
    // ├─ 3: 6
    // ├─ 4: 7
    // ├─ 4: 2
    // ├─ 4: 6
    // ├─ 5: 8
    // ├─ 5: 2
    // ├─ 5: 9
    // ╰- 5: 1
    val bytes = "100518062007200220062a0408020901"
    val wireMessage: EasterK3 = EasterK3.ADAPTER.decode(bytes.decodeHex())
    val protocMessage: EasterP3 = EasterP3.parseFrom(bytes.decodeHex().toByteArray())

    val checker = InteropChecker(
      protocMessage = protocMessage,
      canonicalJson = """{"optionalEasterAnimal":5,"identityEasterAnimal":6,"easterAnimalsRepeated":[7,"HEN",6],"easterAnimalsPacked":[8,"HEN",9,"BUNNY"]}""",
    )

    checker.check(wireMessage)
  }

  @Test fun buildersOnlyMessage() {
    val checker = InteropChecker(
      protocMessage = BuildersOnlyMessageP2.newBuilder()
        .setBuilder("my_builder")
        .setData(64)
        .addAllMessage(listOf(33, 806))
        .setNestedMessage(NestedMessageP2.newBuilder().setA(99).build())
        .setInt32(32)
        .setValue(95)
        .addAllInt64(listOf(94440L, 77000L, 79510L, 44880L))
        .putAllMap(mapOf("one" to "un", "two" to "deux"))
        .build(),
      canonicalJson = """{"builder":"my_builder","data":64,"message":[33,806],"nestedMessage":{"a":99},"int64":["94440","77000","79510","44880"],"map":{"one":"un","two":"deux"},"int32":32,"value":95}""",
      wireCanonicalJson = """{"builder":"my_builder","data":64,"message":[33,806],"nested_message":{"a":99},"int64":[94440,77000,79510,44880],"map":{"one":"un","two":"deux"},"int32":32,"value":95}""",
    )

    val wireMessage = BuildersOnlyMessageK2.Builder()
      .builder_("my_builder")
      .data_(64)
      .message(listOf(33, 806))
      .squareup_proto2_kotlin_buildersonly_int32(32)
      .package_(OneOf(PACKAGE_VALUE, 95))
      .map(mapOf("one" to "un", "two" to "deux"))
      .squareup_proto2_kotlin_buildersonly_int64(listOf(94440L, 77000L, 79510L, 44880L))
      .nested_message(NestedMessageK2.Builder().a(99).build())
      .build()
    checker.check(wireMessage)
  }

  @Test fun wrappers() {
    val checker = InteropChecker(
      protocMessage = InteropWrappers.newBuilder()
        .setDoubleValue(1.0.toDoubleValue())
        .setFloatValue(2f.toFloatValue())
        .setInt64Value(3L.toInt64Value())
        .setUint64Value(4L.toUInt64Value())
        .setInt32Value(5.toInt32Value())
        .setUint32Value(6.toUInt32Value())
        .setBoolValue(true.toBoolValue())
        .setStringValue("string".toStringValue())
        .setBytesValue(ByteString.of(1).toBytesValue())
        .build(),
      canonicalJson = """{"doubleValue":1.0,"floatValue":2.0,"int64Value":"3","uint64Value":"4","int32Value":5,"uint32Value":6,"boolValue":true,"stringValue":"string","bytesValue":"AQ=="}""",
    )
    checker.check(
      InteropWrappersJ3.Builder()
        .double_value(1.0)
        .float_value(2f)
        .int64_value(3L)
        .uint64_value(4L)
        .int32_value(5)
        .uint32_value(6)
        .bool_value(true)
        .string_value("string")
        .bytes_value(ByteString.of(1))
        .build(),
    )
    checker.check(
      InteropWrappersK3.Builder()
        .double_value(1.0)
        .float_value(2f)
        .int64_value(3L)
        .uint64_value(4L)
        .int32_value(5)
        .uint32_value(6)
        .bool_value(true)
        .string_value("string")
        .bytes_value(ByteString.of(1))
        .build(),
    )
  }
}
