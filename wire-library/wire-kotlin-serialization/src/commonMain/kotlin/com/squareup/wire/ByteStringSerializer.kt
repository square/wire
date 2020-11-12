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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.ByteString
import okio.ByteString.Companion.decodeBase64

/**
 * Encodes [ByteString] instances as Base64 for JSON.
 */
object ByteStringSerializer : KSerializer<ByteString> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("okio.ByteString", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ByteString {
    return decoder.decodeString().decodeBase64()
        ?: throw SerializationException("unexpected byte string")
  }

  override fun serialize(encoder: Encoder, value: ByteString) {
    encoder.encodeString(value.base64())
  }
}
