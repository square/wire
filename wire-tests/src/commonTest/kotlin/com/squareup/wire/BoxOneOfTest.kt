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

import assertk.assertions.message
import com.squareup.wire.protos.kotlin.Form
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.ByteString.Companion.decodeHex

class BoxOneOfTest {
  @Test
  fun encodeDecode() {
    val choice = OneOf(Form.CHOICE_TEXT_ELEMENT, Form.TextElement("Hello!"))
    val message = Form(choice = choice)

    val expectedBytes = "32080a0648656c6c6f21".decodeHex()
    assertEquals(expectedBytes, Form.ADAPTER.encodeByteString(message))
    assertEquals(message, Form.ADAPTER.decode(expectedBytes))
  }
}
