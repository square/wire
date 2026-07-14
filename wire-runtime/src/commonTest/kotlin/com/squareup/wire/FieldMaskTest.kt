/*
 * Copyright (C) 2026 Square, Inc.
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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex

class FieldMaskTest {
  @Test fun storesPaths() {
    val fieldMask = FieldMask(listOf("user.display_name", "photo"))

    assertThat(fieldMask.paths).containsExactly("user.display_name", "photo")
  }

  @Test fun defaultPathsIsEmpty() {
    assertThat(FieldMask().paths).isEqualTo(emptyList())
  }

  @Test fun pathsAreImmutableCopy() {
    val paths = mutableListOf("user.display_name")
    val fieldMask = FieldMask(paths)

    paths += "photo"

    assertThat(fieldMask.paths).containsExactly("user.display_name")
  }

  @Test fun copy() {
    val fieldMask = FieldMask(listOf("user.display_name"))

    assertThat(fieldMask.copy(paths = listOf("photo"))).isEqualTo(FieldMask(listOf("photo")))
  }

  @Test fun protoAdapterEncodesPaths() {
    val fieldMask = FieldMask(listOf("user.display_name", "photo"))

    assertThat(ProtoAdapter.FIELD_MASK.encodeByteString(fieldMask))
      .isEqualTo("0a11757365722e646973706c61795f6e616d650a0570686f746f".decodeHex())
  }

  @Test fun protoAdapterDecodesPaths() {
    val bytes = "0a11757365722e646973706c61795f6e616d650a0570686f746f".decodeHex()

    assertThat(ProtoAdapter.FIELD_MASK.decode(bytes))
      .isEqualTo(FieldMask(listOf("user.display_name", "photo")))
  }

  @Test fun protoAdapterHasTypeUrl() {
    assertThat(ProtoAdapter.FIELD_MASK.typeUrl)
      .isEqualTo("type.googleapis.com/google.protobuf.FieldMask")
  }
}
