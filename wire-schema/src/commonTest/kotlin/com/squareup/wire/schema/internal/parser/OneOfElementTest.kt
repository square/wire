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
package com.squareup.wire.schema.internal.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement.Kind.STRING
import kotlin.test.Test

class OneOfElementTest {
  internal var location = Location.get("file.proto")

  @Test fun oneOfWithOptions() {
    // spotless:off because spotless will remove the indents (trailing spaces) in the oneof block.
    val elementAsString = """
      |message Message {
      |  // You have to take one.
      |  oneof choice {
      |    option (my_oneof_option) = "Well done";
      |    option (my_other_oneof_option) = "Yet again";
      |  
      |    string one = 1;
      |    string two = 2;
      |  }
      |}
      |""".trimMargin()
    // spotless:on

    val expectedElement = MessageElement(
      location = location.at(1, 1),
      name = "Message",
      oneOfs = listOf(
        OneOfElement(
          location = location.at(3, 3),
          name = "choice",
          documentation = "You have to take one.",
          fields = listOf(
            FieldElement(location.at(7, 5), type = "string", name = "one", tag = 1),
            FieldElement(location.at(8, 5), type = "string", name = "two", tag = 2),
          ),
          options = listOf(
            OptionElement(name = "my_oneof_option", kind = STRING, value = "Well done", isParenthesized = true),
            OptionElement(name = "my_other_oneof_option", kind = STRING, value = "Yet again", isParenthesized = true),
          ),
        ),
      ),
    )
    val element = ProtoParser.parse(location, elementAsString).types[0] as MessageElement
    assertThat(element).isEqualTo(expectedElement)
    assertThat(element.toSchema()).isEqualTo(elementAsString)
  }
}
