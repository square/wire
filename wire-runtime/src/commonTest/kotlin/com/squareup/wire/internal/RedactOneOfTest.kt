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
package com.squareup.wire.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.squareup.wire.FieldMask
import com.squareup.wire.OneOf
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.durationOfSeconds
import com.squareup.wire.ofEpochSecond
import kotlin.test.Test
import okio.ByteString.Companion.encodeUtf8

class RedactOneOfTest {
  private class Key<T>(
    tag: Int,
    adapter: ProtoAdapter<T>,
    declaredName: String,
    redacted: Boolean = false,
  ) : OneOf.Key<T>(tag, adapter, declaredName, redacted)

  private fun <T> unchanged(adapter: ProtoAdapter<T>, value: T) {
    val oneOf = OneOf(Key(1, adapter, "member"), value)
    assertThat(oneOf.redactOneOf()).isEqualTo(oneOf)
  }

  @Test fun nullStaysNull() {
    val oneOf: OneOf<Key<String>, String>? = null
    assertThat(oneOf.redactOneOf()).isNull()
  }

  @Test fun redactedKeyBecomesNull() {
    val strings = OneOf(Key(1, ProtoAdapter.STRING, "secret", redacted = true), "secret")
    assertThat(strings.redactOneOf()).isNull()
    val structs = OneOf(Key(2, ProtoAdapter.STRUCT_MAP, "secret", redacted = true), mapOf("a" to 1.0))
    assertThat(structs.redactOneOf()).isNull()
    val ints = OneOf(Key(3, ProtoAdapter.INT32, "secret", redacted = true), 3)
    assertThat(ints.redactOneOf()).isNull()
  }

  @Test fun scalarMembersAreKept() {
    unchanged(ProtoAdapter.STRING, "text")
    unchanged(ProtoAdapter.BYTES, "bytes".encodeUtf8())
    unchanged(ProtoAdapter.INT32, 3)
    unchanged(ProtoAdapter.INT64, 3L)
    unchanged(ProtoAdapter.UINT32, 3)
    unchanged(ProtoAdapter.SINT64, 3L)
    unchanged(ProtoAdapter.FIXED32, 3)
    unchanged(ProtoAdapter.SFIXED64, 3L)
    unchanged(ProtoAdapter.BOOL, true)
    unchanged(ProtoAdapter.FLOAT, 1.5f)
    unchanged(ProtoAdapter.DOUBLE, 1.5)
  }

  @Test fun wrapperMembersAreKept() {
    unchanged(ProtoAdapter.DOUBLE_VALUE, 1.5)
    unchanged(ProtoAdapter.FLOAT_VALUE, 1.5f)
    unchanged(ProtoAdapter.INT64_VALUE, 3L)
    unchanged(ProtoAdapter.UINT64_VALUE, 3L)
    unchanged(ProtoAdapter.INT32_VALUE, 3)
    unchanged(ProtoAdapter.UINT32_VALUE, 3)
    unchanged(ProtoAdapter.BOOL_VALUE, true)
    unchanged(ProtoAdapter.STRING_VALUE, "text")
    unchanged(ProtoAdapter.BYTES_VALUE, "bytes".encodeUtf8())
    unchanged(ProtoAdapter.STRING_VALUE, null)
  }

  @Test fun structNullIsKept() {
    unchanged(ProtoAdapter.STRUCT_NULL, null)
  }

  @Test fun structMemberRedactsStrings() {
    val struct = mapOf("name" to "secret", "count" to 3.0, "ok" to true, "none" to null)
    val oneOf = OneOf(Key(1, ProtoAdapter.STRUCT_MAP, "struct"), struct)
    val expected = mapOf("name" to null, "count" to 3.0, "ok" to true, "none" to null)
    assertThat(oneOf.redactOneOf()).isEqualTo(OneOf(oneOf.key, expected))
  }

  @Test fun listValueMemberRedactsStrings() {
    val list = listOf("secret", 3.0, true, null)
    val oneOf = OneOf(Key(1, ProtoAdapter.STRUCT_LIST, "list"), list)
    assertThat(oneOf.redactOneOf()).isEqualTo(OneOf(oneOf.key, listOf(null, 3.0, true, null)))
  }

  @Test fun valueMemberRedactsStrings() {
    val key = Key(1, ProtoAdapter.STRUCT_VALUE, "value")
    assertThat(OneOf(key, "secret").redactOneOf()).isEqualTo(OneOf(key, null))
    assertThat(OneOf(key, 3.0).redactOneOf()).isEqualTo(OneOf(key, 3.0))
    assertThat(OneOf(key, false).redactOneOf()).isEqualTo(OneOf(key, false))
    assertThat(OneOf(key, null).redactOneOf()).isEqualTo(OneOf(key, null))
    assertThat(OneOf(key, listOf("secret")).redactOneOf()).isEqualTo(OneOf(key, listOf(null)))
  }

  @Test fun timeAndEmptyAndFieldMaskMembersAreKept() {
    unchanged(ProtoAdapter.DURATION, durationOfSeconds(1L, 2L))
    unchanged(ProtoAdapter.INSTANT, ofEpochSecond(1L, 2L))
    unchanged(ProtoAdapter.EMPTY, Unit)
    unchanged(ProtoAdapter.FIELD_MASK, FieldMask(listOf("a.b")))
  }

  @Test fun toStringPrintsValueOfMemberThatIsNotRedacted() {
    assertThat(OneOf(Key(1, ProtoAdapter.INT32, "count"), 3).toString()).isEqualTo("count=3")
    assertThat(OneOf(Key(2, ProtoAdapter.STRING, "name"), "a\nb").toString()).isEqualTo("name=a\\nb")
  }

  @Test fun toStringPrintsPlaceholderForRedactedMember() {
    val secret = OneOf(Key(1, ProtoAdapter.STRING, "secret", redacted = true), "secret")
    assertThat(secret.toString()).isEqualTo("secret=██")
    val struct = OneOf(Key(2, ProtoAdapter.STRUCT_MAP, "struct", redacted = true), mapOf("a" to "b"))
    assertThat(struct.toString()).isEqualTo("struct=██")
  }
}
