/*
 * Copyright 2021 Square Inc.
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
package com.squareup.wire.kotlinxserialization

import com.squareup.wire.ProtoAdapter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Returns a serializer that emits values as a proto-encoded byte array. */
fun <E : Any> ProtoAdapter<E>.asSerializer(): KSerializer<E> {
  return WireSerializer(this)
}

@OptIn(ExperimentalSerializationApi::class)
private class WireSerializer<E : Any>(
  val protoAdapter: ProtoAdapter<E>
) : KSerializer<E> {
  private val delegate = ByteArraySerializer()

  override val descriptor = SerialDescriptor("WireSerializer", delegate.descriptor)

  override fun serialize(encoder: Encoder, value: E) {
    val byteArray = protoAdapter.encode(value)
    encoder.encodeSerializableValue(delegate, byteArray)
  }

  override fun deserialize(decoder: Decoder): E {
    val byteArray = decoder.decodeSerializableValue(delegate)
    return protoAdapter.decode(byteArray)
  }
}
