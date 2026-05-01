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
package com.squareup.wire.internal

import com.squareup.wire.KotlinConstructorBuilder
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireField
import com.squareup.wire.WireOneofField
import java.lang.reflect.Field

internal class SealedOneOfBinding<M : Message<M, B>, B : Message.Builder<M, B>>(
  private val messageField: Field,
  builderType: Class<B>,
  private val annotation: WireOneofField,
  private val subclassType: Class<*>,
  classLoader: ClassLoader? = messageField.declaringClass.classLoader,
) : FieldOrOneOfBinding<M, B>() {

  init {
    messageField.isAccessible = true
  }

  // When there is no explicit Builder (javaInterop = false), KotlinConstructorBuilder is used.
  // It has no message-specific fields, so we store sealed oneof values in its side-map instead.
  private val isKotlinConstructorBuilder: Boolean =
    builderType == KotlinConstructorBuilder::class.java

  private val builderField: Field? = if (isKotlinConstructorBuilder) {
    null
  } else {
    builderType.getDeclaredField(messageField.name).also { it.isAccessible = true }
  }

  /** @see WireOneofField.tag */
  override val tag: Int get() = annotation.tag

  /** Sealed oneof fields are always optional. Equivalent to [WireField.Label.OPTIONAL]. */
  override val label: WireField.Label get() = WireField.Label.OPTIONAL

  /** @see WireOneofField.redacted */
  override val redacted: Boolean get() = annotation.redacted

  /** @see WireOneofField.jsonName */
  override val wireFieldJsonName: String get() = annotation.jsonName

  /** @see WireOneofField.declaredName */
  override val name: String get() = annotation.declaredName

  /** @see WireOneofField.declaredName */
  override val declaredName: String get() = annotation.declaredName

  override val isMap: Boolean get() = false
  override val isMessage: Boolean
    get() = Message::class.java.isAssignableFrom(singleAdapter.type?.javaObjectType)
  override val keyAdapter: ProtoAdapter<*> get() = error("not a map")

  /** @see WireOneofField.adapter */
  @Suppress("UNCHECKED_CAST")
  override val singleAdapter: ProtoAdapter<*> =
    ProtoAdapter.get(annotation.adapter, classLoader) as ProtoAdapter<Any>
  override val writeIdentityValues: Boolean get() = false

  private val valueField: Field by lazy {
    subclassType.getDeclaredField("value").also { it.isAccessible = true }
  }

  override fun get(message: M): Any? {
    val sealed = messageField.get(message) ?: return null
    if (!subclassType.isInstance(sealed)) return null
    return valueField.get(sealed)
  }

  override fun getFromBuilder(builder: B): Any? {
    val sealed = if (isKotlinConstructorBuilder) {
      @Suppress("UNCHECKED_CAST")
      (builder as KotlinConstructorBuilder<M, B>).getSealedOneof(messageField.name)
    } else {
      builderField!!.get(builder)
    } ?: return null
    if (!subclassType.isInstance(sealed)) return null
    return valueField.get(sealed)
  }

  override fun set(builder: B, value: Any?) {
    if (value == null) return
    val ctor = subclassType.declaredConstructors.first { it.parameterCount == 1 }
    ctor.isAccessible = true
    val sealed = ctor.newInstance(value)
    if (isKotlinConstructorBuilder) {
      @Suppress("UNCHECKED_CAST")
      (builder as KotlinConstructorBuilder<M, B>).setSealedOneof(messageField.name, sealed)
    } else {
      builderField!!.set(builder, sealed)
    }
  }

  override fun value(builder: B, value: Any) = set(builder, value)
}
