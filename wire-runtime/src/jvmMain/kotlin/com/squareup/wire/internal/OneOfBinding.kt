/*
 * Copyright (C) 2021 Square, Inc.
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

import com.squareup.wire.Message
import com.squareup.wire.OneOf
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireField
import java.lang.reflect.Field

internal class OneOfBinding<M : Message<M, B>, B : Message.Builder<M, B>> internal constructor(
  private val messageField: Field,
  builderType: Class<B>,
  private val key: OneOf.Key<*>,
  override val writeIdentityValues: Boolean,
) : FieldOrOneOfBinding<M, B>() {
  private val builderField: Field = builderType.getDeclaredField(messageField.name)

  override val tag: Int
    get() = key.tag

  override val label: WireField.Label
    get() = WireField.Label.OPTIONAL

  override val redacted: Boolean
    get() = key.redacted

  override val wireFieldJsonName: String
    get() = key.jsonName

  override val name: String
    get() = key.declaredName

  override val declaredName: String
    get() = key.declaredName

  override val isMap: Boolean
    get() = false

  override val isMessage: Boolean
    get() = Message::class.java.isAssignableFrom(singleAdapter.type?.javaObjectType)

  override val keyAdapter
    get() = error("not a map")

  @Suppress("UNCHECKED_CAST")
  override val singleAdapter
    get() = key.adapter as ProtoAdapter<Any>

  override fun value(builder: B, value: Any) {
    set(builder, value)
  }

  override fun set(builder: B, value: Any?) {
    @Suppress("UNCHECKED_CAST")
    builderField.set(builder, OneOf(key as OneOf.Key<Any>, value!!))
  }

  override fun get(message: M): Any? {
    val oneOfOrNull = messageField.get(message) as OneOf<*, *>?
    return oneOfOrNull?.getOrNull(key)
  }

  override fun getFromBuilder(builder: B): Any? {
    val oneOfOrNull = builderField.get(builder) as OneOf<*, *>?
    return oneOfOrNull?.getOrNull(key)
  }
}
