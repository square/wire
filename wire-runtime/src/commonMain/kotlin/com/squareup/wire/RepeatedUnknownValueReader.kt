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
 * [UnknownValueReader] which reads the all values whose tag matches [targetTag]. The values will
 * be decoded by the [adapter]. If you don't know have the value type on your classpath, you can use
 * simple adapters like [ProtoAdapter.BYTES]. For enums whose constants are not known to the
 * application, use [ProtoAdapter.INT32].
 */
class RepeatedUnknownValueReader<M : Message<M, *>, V : Any>(
  private val targetTag: Int,
  private val adapter: ProtoAdapter<V>,
) : UnknownValueReader<M, List<V>> {
  override fun read(message: M): List<V> {
    val result = mutableListOf<V>()
    val reader = ProtoReader(Buffer().write(message.unknownFields))
    reader.forEachTag { tag ->
      if (tag != targetTag) {
        reader.skip()
      } else {
        result += adapter.decode(reader)
      }
    }
    return result
  }
}
