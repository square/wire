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
@file:JvmName("PrimitiveArraysJvm")

package com.squareup.wire.internal

/**
 * Returns [value] as an instance of [type] if [type] is a primitive array class and [value] is a
 * list. Returns [value] as is in every other case.
 */
internal fun listAsPrimitiveArray(type: Class<*>, value: Any?): Any? {
  if (value !is List<*>) return value
  return when (type) {
    IntArray::class.java -> IntArray(value.size) { value[it] as Int }
    LongArray::class.java -> LongArray(value.size) { value[it] as Long }
    FloatArray::class.java -> FloatArray(value.size) { value[it] as Float }
    DoubleArray::class.java -> DoubleArray(value.size) { value[it] as Double }
    else -> value
  }
}
