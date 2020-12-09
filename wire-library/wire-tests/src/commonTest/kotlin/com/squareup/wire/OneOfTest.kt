/*
 * Copyright 2015 Square Inc.
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

import com.squareup.wire.protos.kotlin.OneOfMessage2 as OneOfMessage
import okio.ByteString.Companion.decodeHex
import kotlin.test.Test
import kotlin.test.assertEquals

class OneOfTest {
  @Test
  fun constructorFailsWhenBothFieldsAreNonNull() {
//    try {
//      OneOfMessage(foo = 1, bar = "two", baz = null)
//      fail()
//    } catch (expected: IllegalArgumentException) {
//      assertEquals("At most one of foo, bar, baz may be non-null", expected.message)
//    }
  }

  @Test
  fun encodeDecode() {

    val choice = OneOf(OneOfMessage.choiceBar, "hello")
    val message = OneOfMessage(choice = choice)

    val s = when (message.choice?.key) {
      OneOfMessage.choiceBar -> "bar"
      OneOfMessage.choiceBaz -> "baz"
      OneOfMessage.choiceFoo -> "foo"
      null -> "none"
      else -> error("unexpected choice")
    }

    val expectedBytes = "1a0568656c6c6f".decodeHex()
    assertEquals(expectedBytes, OneOfMessage.ADAPTER.encodeByteString(message))
    assertEquals(message, OneOfMessage.ADAPTER.decode(expectedBytes))

//    try {
//      OneOfMessage(foo = 1, bar = "two", baz = null)
//      fail()
//    } catch (expected: IllegalArgumentException) {
//      assertEquals("At most one of foo, bar, baz may be non-null", expected.message)
//    }
  }
}
