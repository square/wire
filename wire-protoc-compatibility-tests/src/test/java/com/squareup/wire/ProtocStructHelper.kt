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

import com.google.protobuf.ListValue
import com.google.protobuf.NullValue
import com.google.protobuf.Struct
import com.google.protobuf.Value

internal fun Map<String, *>.toStruct(): Struct {
  val builder = Struct.newBuilder()
  for ((key, value) in this) {
    builder.putFields(key, value.toValue())
  }
  return builder.build()
}

internal fun emptyStruct(): Struct {
  return Struct.newBuilder().build()
}

internal fun List<*>.toListValue(): ListValue {
  val builder = ListValue.newBuilder()
  for (any in this) {
    builder.addValues(any.toValue())
  }
  return builder.build()
}

internal fun emptyListValue(): ListValue {
  return ListValue.newBuilder().build()
}

internal fun Any?.toValue(): Value {
  return when (this) {
    null -> Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build()
    is Double -> Value.newBuilder().setNumberValue(this).build()
    is String -> Value.newBuilder().setStringValue(this).build()
    is Boolean -> Value.newBuilder().setBoolValue(this).build()
    is Map<*, *> -> Value.newBuilder().setStructValue((this as Map<String, *>).toStruct()).build()
    is List<*> -> Value.newBuilder().setListValue(toListValue()).build()
    else -> throw IllegalArgumentException("unexpected struct value: $this")
  }
}
