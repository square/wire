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
) {
  val label: WireField.Label = wireField.label
  val name: String = messageField.name
  val tag: Int = wireField.tag
  private val keyAdapterString = wireField.keyAdapter
  private val adapterString = wireField.adapter
  val redacted: Boolean = wireField.redacted
  private val builderField = getBuilderField(builderType, name)
  private val builderMethod = getBuilderMethod(builderType, name, messageField.type)

  // Delegate adapters are created lazily; otherwise we could stack overflow!
  private var singleAdapter: ProtoAdapter<*>? = null
  private var keyAdapter: ProtoAdapter<*>? = null
  private var adapter: ProtoAdapter<Any>? = null

  val isMap: Boolean
    get() = keyAdapterString.isNotEmpty()

  private fun getBuilderField(builderType: Class<*>, name: String): Field {
    try {
      return builderType.getField(name)
    } catch (_: NoSuchFieldException) {
      throw AssertionError("No builder field ${builderType.name}.$name")
    }
  }

  private fun getBuilderMethod(builderType: Class<*>, name: String, type: Class<*>): Method {
    try {
      return builderType.getMethod(name, type)
    } catch (_: NoSuchMethodException) {
      throw AssertionError("No builder method ${builderType.name}.$name(${type.name})")
    }
  }

  fun singleAdapter(): ProtoAdapter<*> {
    return singleAdapter ?: ProtoAdapter.get(adapterString).also { singleAdapter = it }
  }

  fun keyAdapter(): ProtoAdapter<*> {
    return keyAdapter ?: ProtoAdapter.get(keyAdapterString).also { keyAdapter = it }
  }

  internal fun adapter(): ProtoAdapter<Any> {
    val result = adapter
    if (result != null) return result
    if (isMap) {
      val keyAdapter = keyAdapter() as ProtoAdapter<Any>
      val valueAdapter = singleAdapter() as ProtoAdapter<Any>
      return (ProtoAdapter.newMapAdapter(keyAdapter, valueAdapter) as ProtoAdapter<Any>).also {
        adapter = it
      }
    }
    return (singleAdapter().withLabel(label) as ProtoAdapter<Any>).also { adapter = it }
  }

  /** Accept a single value, independent of whether this value is single or repeated. */
  internal fun value(builder: B, value: Any) {
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
  operator fun set(builder: B, value: Any?) {
    if (label.isOneOf) {
      // In order to maintain the 'oneof' invariant, call the builder setter method rather
      // than setting the builder field directly.
      builderMethod.invoke(builder, value)
    } else {
      builderField.set(builder, value)
    }
  }

  operator fun get(message: M): Any? = messageField.get(message)

  internal fun getFromBuilder(builder: B): Any? = builderField.get(builder)
}
