/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.internal.parser.ExtendElement
import com.squareup.wire.schema.internal.parser.FieldElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.OptionElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import com.squareup.wire.schema.internal.parser.RpcElement
import com.squareup.wire.schema.internal.parser.ServiceElement
import kotlin.test.Test

class ProtoFileTest {
  internal var location = Location.get("file.proto")

  @Test
  fun roundTripToElement() {
    val element1 = MessageElement(
      location = location.at(11, 1),
      name = "Message1",
      documentation = "Some comments about Message1",
    )
    val element2 = MessageElement(
      location = location.at(12, 1),
      name = "Message2",
      fields = listOf(
        FieldElement(
          location = location.at(13, 3),
          type = "string",
          name = "field",
          tag = 1,
        ),
      ),
    )

    val extend1 = ExtendElement(
      location = location.at(16, 1),
      name = "Extend1",
    )
    val extend2 = ExtendElement(
      location = location.at(17, 1),
      name = "Extend2",
    )
    val option1 = OptionElement.create("kit", OptionElement.Kind.STRING, "kat")
    val option2 = OptionElement.create("foo", OptionElement.Kind.STRING, "bar")
    val service1 = ServiceElement(
      location = location.at(19, 1),
      name = "Service1",
      rpcs = listOf(
        RpcElement(
          location = location.at(20, 3),
          name = "MethodA",
          requestType = "Message2",
          responseType = "Message1",
          options = listOf(
            OptionElement.create("methodoption", OptionElement.Kind.NUMBER, 1),
          ),
        ),
      ),
    )
    val service2 = ServiceElement(
      location = location.at(24, 1),
      name = "Service2",
    )
    val fileElement = ProtoFileElement(
      location = location,
      packageName = "example.simple",
      imports = listOf("example.thing"),
      publicImports = listOf("example.other"),
      types = listOf(element1, element2),
      services = listOf(service1, service2),
      extendDeclarations = listOf(extend1, extend2),
      options = listOf(option1, option2),
    )
    val file = ProtoFile.get(fileElement)

    val expected = """
        |// Proto schema formatted by Wire, do not edit.
        |// Source: file.proto
        |
        |package example.simple;
        |
        |import "example.thing";
        |import public "example.other";
        |
        |option kit = "kat";
        |option foo = "bar";
        |
        |// Some comments about Message1
        |message Message1 {}
        |
        |message Message2 {
        |  string field = 1;
        |}
        |
        |extend Extend1 {}
        |
        |extend Extend2 {}
        |
        |service Service1 {
        |  rpc MethodA (Message2) returns (Message1) {
        |    option methodoption = 1;
        |  };
        |}
        |
        |service Service2 {}
        |
    """.trimMargin()

    assertThat(file.toSchema()).isEqualTo(expected)
    assertThat(file.toElement()).isEqualTo(fileElement)
  }
}
