/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.google.protobuf.Duration
import org.junit.Test
import squareup.proto2.java.interop.InteropCamelCase as InteropCamelCaseJ2
import squareup.proto2.java.interop.InteropDuration as InteropDurationJ2
import squareup.proto2.java.interop.InteropJsonName as InteropJsonNameJ2
import squareup.proto2.java.interop.InteropTest.InteropCamelCase as InteropCamelCaseP2
import squareup.proto2.java.interop.InteropTest.InteropJsonName as InteropJsonNameP2
import squareup.proto2.java.interop.InteropTest.InteropUint64 as InteropUint64P2
import squareup.proto2.java.interop.InteropUint64 as InteropUint64J2
import squareup.proto2.kotlin.interop.InteropCamelCase as InteropCamelCaseK2
import squareup.proto2.kotlin.interop.InteropDuration as InteropDurationK2
import squareup.proto2.kotlin.interop.InteropJsonName as InteropJsonNameK2
import squareup.proto2.kotlin.interop.InteropUint64 as InteropUint64K2
import squareup.proto3.java.interop.InteropCamelCase as InteropCamelCaseJ3
import squareup.proto3.java.interop.InteropDuration as InteropDurationJ3
import squareup.proto3.java.interop.InteropJsonName as InteropJsonNameJ3
import squareup.proto3.java.interop.InteropTest.InteropCamelCase as InteropCamelCaseP3
import squareup.proto3.java.interop.InteropTest.InteropDuration as InteropDurationP3
import squareup.proto3.java.interop.InteropTest.InteropJsonName as InteropJsonNameP3
import squareup.proto3.java.interop.InteropTest.InteropUint64 as InteropUint64P3
import squareup.proto3.java.interop.InteropUint64 as InteropUint64J3
import squareup.proto3.kotlin.interop.InteropCamelCase as InteropCamelCaseK3
import squareup.proto3.kotlin.interop.InteropDuration as InteropDurationK3
import squareup.proto3.kotlin.interop.InteropJsonName as InteropJsonNameK3
import squareup.proto3.kotlin.interop.InteropUint64 as InteropUint64K3

class InteropTest {
  @Test fun duration() {
    val checker = InteropChecker(
        protocMessage = InteropDurationP3.newBuilder()
            .setValue(Duration.newBuilder()
                .setSeconds(99L)
                .setNanos(987_654_321)
                .build())
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
            """{"value":"-1"}"""
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
            """{"value":"-1"}"""
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
            .build(),
        canonicalJson = """{"helloWorld":"1","aB":"2","CccDdd":"3","eEeeFfGGg":"4"}""",
        alternateJsons = listOf(
            """{"hello_world": "1", "a__b": "2", "_Ccc_ddd": "3", "EEee_ff_gGg": "4"}""",
        )
    )

    checker.check(InteropCamelCaseK3("1", "2", "3", "4"))
    checker.check(InteropCamelCaseJ3("1", "2", "3", "4"))
  }

  @Test fun `camel case proto 2`() {
    val checker = InteropChecker(
        protocMessage = InteropCamelCaseP2.newBuilder()
            .setHelloWorld("1")
            .setAB("2")
            .setCccDdd("3")
            .setEEeeFfGGg("4")
            .build(),
        canonicalJson = """{"helloWorld":"1","aB":"2","CccDdd":"3","eEeeFfGGg":"4"}""",
        wireCanonicalJson = """{"hello_world":"1","a__b":"2","_Ccc_ddd":"3","EEee_ff_gGg":"4"}""",
        alternateJsons = listOf(
            // TODO(bquenaudon): support reading camelCase proto2 messages.
            // """{"helloWorld":"1","aB":"2","CccDdd":"3","eEeeFfGGg":"4"}""",
        )
    )

    checker.check(InteropCamelCaseK2("1", "2", "3", "4"))
    checker.check(InteropCamelCaseJ2("1", "2", "3", "4"))
  }

  @Test fun `json names`() {
    val checked = InteropChecker(
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

    checked.check(InteropJsonNameJ3("1", "2", "3"))
    checked.check(InteropJsonNameK3("1", "2", "3"))
  }

  @Test fun `json names proto2`() {
    val checked = InteropChecker(
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

    checked.check(InteropJsonNameJ2("1", "2", "3"))
    checked.check(InteropJsonNameK2("1", "2", "3"))
  }
}

