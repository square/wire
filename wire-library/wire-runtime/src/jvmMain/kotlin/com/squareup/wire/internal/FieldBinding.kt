/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire.internal

import com.squareup.wire.KotlinConstructorBuilder
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireField
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Read, write, and describe a tag within a message. This class knows how to assign fields to a
 * builder object, and how to extract values from a message object.
 */
class FieldBinding<M : Message<M, B>, B : Message.Builder<M, B>> internal constructor(
  wireField: WireField,
  private val messageField: Field,
  builderType: Class<B>
) : FieldOrOneOfBinding<M, B>() {
  override val label: WireField.Label = wireField.label
  override val name: String = messageField.name
  override val wireFieldJsonName: String = wireField.jsonName
  override val declaredName: String =
      if (wireField.declaredName.isEmpty()) messageField.name else wireField.declaredName
  override val tag: Int = wireField.tag
  private val keyAdapterString = wireField.keyAdapter
  private val adapterString = wireField.adapter
  override val redacted: Boolean = wireField.redacted
  private val builderSetter = getBuilderSetter(builderType, wireField)
  private val builderGetter = getBuilderGetter(builderType, wireField)

  override val keyAdapter: ProtoAdapter<*>
    get() = ProtoAdapter.get(keyAdapterString)
  override val singleAdapter: ProtoAdapter<*>
    get() = ProtoAdapter.get(adapterString)

  override val isMap: Boolean
    get() = keyAdapterString.isNotEmpty()

  override val isMessage: Boolean
    get() = Message::class.java.isAssignableFrom(singleAdapter.type?.javaObjectType)

  private fun getBuilderSetter(builderType: Class<*>, wireField: WireField): (B, Any?) -> Unit {
    return when {
      builderType.isAssignableFrom(KotlinConstructorBuilder::class.java) -> { builder, value ->
        (builder as KotlinConstructorBuilder<*, *>).set(wireField, value)
      }
      wireField.label.isOneOf -> {
        val type = messageField.type
        val method = try {
          builderType.getMethod(name, type)
        } catch (_: NoSuchMethodException) {
          throw AssertionError("No builder method ${builderType.name}.$name(${type.name})")
        }
        { builder, value ->
          method.invoke(builder, value)
        }
      }
      else -> {
        val field = try {
          builderType.getField(name)
        } catch (_: NoSuchFieldException) {
          throw AssertionError("No builder field ${builderType.name}.$name")
        }
        { builder, value ->
          field.set(builder, value)
        }
      }
    }
  }

  private fun getBuilderGetter(builderType: Class<*>, wireField: WireField): (B) -> Any? {
    return if (builderType.isAssignableFrom(KotlinConstructorBuilder::class.java)) {
      { builder ->
        (builder as KotlinConstructorBuilder<*, *>).get(wireField)
      }
    } else {
      val field = try {
        builderType.getField(name)
      } catch (_: NoSuchFieldException) {
        throw AssertionError("No builder field ${builderType.name}.$name")
      }
      { builder ->
        field.get(builder)
      }
    }
  }

  /** Accept a single value, independent of whether this value is single or repeated. */
  override fun value(builder: B, value: Any) {
    when {
      label.isRepeated -> {
        when (val list = getFromBuilder(builder)) {
          is MutableList<*> -> (list as MutableList<Any>).add(value)
          is List<*> -> {
            val mutableList = list.toMutableList()
            mutableList.add(value)
            set(builder, mutableList)
          }
          else -> {
            val type = list?.let { it::class.java }
            throw ClassCastException("Expected a list type, got $type.")
          }
        }
      }
      keyAdapterString.isNotEmpty() -> {
        when (val map = getFromBuilder(builder)) {
          is MutableMap<*, *> -> map.putAll(value as Map<Nothing, Nothing>)
          is Map<*, *> -> {
            val mutableMap = map.toMutableMap()
            mutableMap.putAll(value as Map<out Any?, Any?>)
            set(builder, mutableMap)
          }
          else -> {
            val type = map?.let { it::class.java }
            throw ClassCastException("Expected a map type, got $type.")
          }
        }
      }
      else -> set(builder, value)
    }
  }

  /** Assign a single value for required/optional fields, or a list for repeated/packed fields. */
  override fun set(builder: B, value: Any?) = builderSetter(builder, value)

  override operator fun get(message: M): Any? = messageField.get(message)

  override fun getFromBuilder(builder: B): Any? = builderGetter(builder)
}
