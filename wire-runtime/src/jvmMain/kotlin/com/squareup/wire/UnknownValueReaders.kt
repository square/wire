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

import kotlin.reflect.KProperty1

/**
 * Helper method to get an [UnknownValueReader] which will read the unknown value for the field [P].
 */
inline fun <reified M : Message<M, *>, P : Enum<P>> KProperty1<M, P?>.unknownValueReader(): UnknownValueReader<M, Int?> {
  val targetTag = M::class.java.getField(name).getAnnotation(WireField::class.java).tag
  return SingleUnknownValueReader(targetTag, ProtoAdapter.INT32)
}

/**
 * Helper method to get an [UnknownValueReader] which will read all unknown values for the field [P].
 */
inline fun <reified M : Message<M, *>, P : Enum<P>> KProperty1<M, List<P>>.unknownValuesReader(): UnknownValueReader<M, List<Int>> {
  val targetTag = M::class.java.getField(name).getAnnotation(WireField::class.java).tag
  return RepeatedUnknownValueReader(targetTag, ProtoAdapter.INT32)
}
