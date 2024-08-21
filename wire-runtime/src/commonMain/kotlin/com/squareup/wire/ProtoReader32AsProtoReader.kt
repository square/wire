/*
 * Copyright (C) 2024 Square, Inc.
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

import okio.Buffer

/**
 * Adapts a [ProtoReader32] as a [ProtoReader] so that [ProtoAdapter] implementations that don't
 * have generated code for [ProtoReader32] forms can still be used with that type.
 *
 * This code is fragile because [ProtoReader] was not designed to be subclassed this way. In
 * particular none of the state variables in the supertype [ProtoReader] are used in any way. (If
 * we could break backwards-compatibility, we would make [ProtoReader] an interface.)
 */
internal class ProtoReader32AsProtoReader(
  val delegate: ProtoReader32,
) : ProtoReader(Buffer()) {
  override fun beginMessage() = delegate.beginMessage().toLong()

  override fun endMessageAndGetUnknownFields(token: Long) =
    delegate.endMessageAndGetUnknownFields(token.toInt())

  override fun nextLengthDelimited() = delegate.nextLengthDelimited()

  override fun nextTag() = delegate.nextTag()

  override fun peekFieldEncoding() = delegate.peekFieldEncoding()

  override fun skip() {
    delegate.skip()
  }

  override fun readBytes() = delegate.readBytes()

  override fun beforePossiblyPackedScalar() = delegate.beforePossiblyPackedScalar()

  override fun readString() = delegate.readString()

  override fun readVarint32() = delegate.readVarint32()

  override fun readVarint64() = delegate.readVarint64()

  override fun readFixed32() = delegate.readFixed32()

  override fun readFixed64() = delegate.readFixed64()

  override fun readUnknownField(tag: Int) {
    delegate.readUnknownField(tag)
  }

  override fun addUnknownField(tag: Int, fieldEncoding: FieldEncoding, value: Any?) {
    delegate.addUnknownField(tag, fieldEncoding, value)
  }

  override fun nextFieldMinLengthInBytes() = delegate.nextFieldMinLengthInBytes().toLong()
}
