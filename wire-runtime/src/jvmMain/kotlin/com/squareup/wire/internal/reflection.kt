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

import com.squareup.wire.FieldEncoding
import com.squareup.wire.KotlinConstructorBuilder
import com.squareup.wire.Message
import com.squareup.wire.OneOf
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax
import com.squareup.wire.WireField
import java.lang.reflect.Field
import java.util.Collections
import kotlin.reflect.KClass

fun <M : Message<M, B>, B : Message.Builder<M, B>> createRuntimeMessageAdapter(
  messageType: Class<M>,
  typeUrl: String?,
  syntax: Syntax,
  classLoader: ClassLoader? = messageType.classLoader,
  writeIdentityValues: Boolean = false,
): RuntimeMessageAdapter<M, B> {
  val builderType = getBuilderType(messageType)
  val newBuilderInstance: () -> B = {
    if (builderType.isAssignableFrom(KotlinConstructorBuilder::class.java)) {
      @Suppress("UNCHECKED_CAST")
      KotlinConstructorBuilder(messageType) as B
    } else {
      @Suppress("DEPRECATION")
      builderType.newInstance()
    }
  }

  val fields = LinkedHashMap<Int, FieldOrOneOfBinding<M, B>>()

  // Create tag bindings for fields annotated with '@WireField'.
  for (messageField in messageType.declaredFields) {
    val wireField = messageField.getAnnotation(WireField::class.java)
    if (wireField != null) {
      fields[wireField.tag] = FieldBinding(
        wireField,
        messageType,
        messageField,
        builderType,
        writeIdentityValues,
        classLoader,
      )
    } else if (messageField.type == OneOf::class.java) {
      for (key in getKeys<M, B>(messageField)) {
        fields[key.tag] = OneOfBinding(messageField, builderType, key, writeIdentityValues)
      }
    }
  }

  return RuntimeMessageAdapter(
    RuntimeMessageBinding(
      messageType.kotlin,
      builderType,
      newBuilderInstance,
      Collections.unmodifiableMap(fields),
      typeUrl,
      syntax,
    ),
  )
}

private fun <M : Message<M, B>, B : Message.Builder<M, B>> getKeys(
  messageField: Field,
): Set<OneOf.Key<*>> {
  val messageClass = messageField.declaringClass
  val keysField = messageClass.getDeclaredField(boxedOneOfKeysFieldName(messageField.name))
  keysField.isAccessible = true
  @Suppress("UNCHECKED_CAST")
  return keysField.get(null) as Set<OneOf.Key<*>>
}

fun <M : Message<M, B>, B : Message.Builder<M, B>> createRuntimeMessageAdapter(
  messageType: Class<M>,
  writeIdentityValues: Boolean,
  classLoader: ClassLoader? = messageType.classLoader,
): RuntimeMessageAdapter<M, B> {
  val defaultAdapter = ProtoAdapter.get(messageType as Class<*>)
  return createRuntimeMessageAdapter(
    messageType = messageType,
    typeUrl = defaultAdapter.typeUrl,
    syntax = defaultAdapter.syntax,
    classLoader = classLoader,
    writeIdentityValues = writeIdentityValues,
  )
}

@Suppress("UNCHECKED_CAST")
private fun <M : Message<M, B>, B : Message.Builder<M, B>> getBuilderType(
  messageType: Class<M>,
): Class<B> {
  return runCatching {
    Class.forName("${messageType.name}\$Builder") as Class<B>
  }
    .getOrNull() ?: KotlinConstructorBuilder::class.java as Class<B>
}

private class RuntimeMessageBinding<M : Message<M, B>, B : Message.Builder<M, B>>(
  override val messageType: KClass<M>,
  private val builderType: Class<B>,
  private val createBuilder: () -> B,
  override val fields: Map<Int, FieldOrOneOfBinding<M, B>>,
  override val typeUrl: String?,
  override val syntax: Syntax,
) : MessageBinding<M, B> {

  override fun unknownFields(message: M) = message.unknownFields

  override fun getCachedSerializedSize(message: M): Int = message.cachedSerializedSize

  override fun setCachedSerializedSize(message: M, size: Int) {
    message.cachedSerializedSize = size
  }

  override fun newBuilder(): B = createBuilder()

  override fun build(builder: B): M {
    return builder.build()
  }

  override fun addUnknownField(builder: B, tag: Int, fieldEncoding: FieldEncoding, value: Any?) {
    builder.addUnknownField(tag, fieldEncoding, value)
  }

  override fun clearUnknownFields(builder: B) {
    builder.clearUnknownFields()
  }
}
