/*
 * Copyright (C) 2016 Square, Inc.
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

import com.squareup.wire.internal.identityOrNull
import java.io.IOException
import kotlin.reflect.KClass

/**
 * An abstract [ProtoAdapter] that converts values of an enum to and from integers.
 */
actual abstract class EnumAdapter<E : WireEnum> protected actual constructor(
  type: KClass<E>,
  syntax: Syntax,
  identity: E?,
) : ProtoAdapter<E>(FieldEncoding.VARINT, type, null, syntax, identity) {
  constructor(type: Class<E>, syntax: Syntax, identity: E?) : this(type.kotlin, syntax, identity)

  // Obsolete; for Java classes generated before syntax was added.
  constructor(type: Class<E>) : this(type.kotlin, Syntax.PROTO_2, type.identityOrNull)

  // Obsolete; for Java classes generated before identity was added.
  constructor(type: Class<E>, syntax: Syntax) : this(type.kotlin, syntax, type.identityOrNull)

  // Obsolete; for Kotlin classes generated before syntax was added.
  constructor(type: KClass<E>) : this(type, Syntax.PROTO_2, type.java.identityOrNull)

  // Obsolete; for Kotlin classes generated before identity was added.
  constructor(type: KClass<E>, syntax: Syntax) : this(type, syntax, type.java.identityOrNull)

  actual override fun encodedSize(value: E): Int = commonEncodedSize(value)

  @Throws(IOException::class)
  actual override fun encode(writer: ProtoWriter, value: E) {
    commonEncode(writer, value)
  }

  @Throws(IOException::class)
  actual override fun encode(writer: ReverseProtoWriter, value: E) {
    commonEncode(writer, value)
  }

  @Throws(IOException::class)
  actual override fun decode(reader: ProtoReader): E = commonDecode(reader, this::fromValue)

  @Throws(IOException::class)
  actual override fun decode(reader: ProtoReader32): E = commonDecode(reader, this::fromValue)

  actual override fun redact(value: E): E = commonRedact(value)

  /**
   * Converts an integer to an enum.
   * Returns null if there is no corresponding enum.
   */
  protected actual abstract fun fromValue(value: Int): E?
}
