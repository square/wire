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
import assertk.assertions.contains
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.ByteString

/**
 * Code that an older Wire generated for a boxed oneof, without a builder, has no [WireBoxedOneof]
 * annotation. The reflective adapters cannot place such a oneof among the constructor parameters,
 * so they fail with a message that tells to regenerate the code.
 */
class OldBoxedOneOfTest {
  @Test fun reflectiveAdapterFailsWithClearMessage() {
    val e = assertFailsWith<IllegalStateException> {
      ProtoAdapter.newMessageAdapter<OldChoice, Nothing>(OldChoice::class.java)
    }
    assertThat(e.message!!).contains("OldChoice.choice is a boxed oneof without @WireBoxedOneof")
    assertThat(e.message!!).contains("Regenerate")
  }

  class OldChoice(
    @JvmField val choice: OneOf<Key<*>, *>? = null,
    unknownFields: ByteString = ByteString.EMPTY,
  ) : Message<OldChoice, Nothing>(ProtoAdapter.STRING as ProtoAdapter<OldChoice>, unknownFields) {
    override fun newBuilder(): Nothing = throw AssertionError()

    class Key<T>(tag: Int, adapter: ProtoAdapter<T>, declaredName: String) : OneOf.Key<T>(tag, adapter, declaredName)

    companion object {
      @JvmField
      val CHOICE_KEYS: Set<Key<*>> = setOf(Key(1, ProtoAdapter.STRING, "text"))
    }
  }
}
