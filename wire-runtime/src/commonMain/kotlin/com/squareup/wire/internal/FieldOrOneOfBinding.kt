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

import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax
import com.squareup.wire.WireField

abstract class FieldOrOneOfBinding<M, B> {
  abstract val tag: Int

  abstract val label: WireField.Label

  abstract val redacted: Boolean

  abstract val isMap: Boolean

  abstract val isMessage: Boolean

  /**
   * The name of the field in generated code. If the declared name is a keyword like `fun`, this
   * will be a transformed name like `fun_`.
   */
  abstract val name: String

  /**
   * The name of the field as declared in the `.proto` file.
   */
  abstract val declaredName: String

  /**
   * The JSON name as determined at code-generation name. This is usually camelCase even if the
   * field is declared in snake_case.
   */
  abstract val wireFieldJsonName: String

  abstract val keyAdapter: ProtoAdapter<*>

  abstract val singleAdapter: ProtoAdapter<*>

  /**
   * If true, Wire will always write identity values.
   */
  abstract val writeIdentityValues: Boolean

  /**
   * The adapter for one element if this field uses a primitive array, like `IntArray`. This is null
   * for every other field.
   */
  internal val primitiveArrayElementAdapter: ProtoAdapter<*>? by lazy {
    singleAdapter.primitiveArrayElementAdapter()
  }

  @Suppress("UNCHECKED_CAST")
  val adapter: ProtoAdapter<Any> by lazy {
    // Delegate adapters are created lazily; otherwise we could stack overflow!
    if (isMap) {
      ProtoAdapter.newMapAdapter(
        keyAdapter as ProtoAdapter<Any>,
        singleAdapter as ProtoAdapter<Any>,
      ) as ProtoAdapter<Any>
    } else if (primitiveArrayElementAdapter != null) {
      // Adapters for primitive arrays are already packed.
      singleAdapter as ProtoAdapter<Any>
    } else {
      singleAdapter.withLabel(label) as ProtoAdapter<Any>
    }
  }

  /** Adds [value] to the builder. The value is null for `google.protobuf.NullValue` and `Value`. */
  abstract fun value(builder: B, value: Any?)

  abstract fun set(builder: B, value: Any?)

  /**
   * Assigns [value], read from JSON, to the builder.
   *
   * Binary decoding keeps the last member that the input sets in a oneof, so [set] does too. For
   * oneofs of generated Java and of generated Kotlin with flat members, JSON decoding instead
   * fails when the input sets two members. Wire 7 did this, and protobuf-java does it too.
   * Other bindings keep the last member, as [set] does.
   */
  open fun setFromJson(builder: B, value: Any?) = set(builder, value)

  abstract operator fun get(message: M): Any?

  abstract fun getFromBuilder(builder: B): Any?

  /**
   * Returns true if the protobuf encoding omits [value].
   * Generated adapters omit the same values.
   */
  fun omitFromProto(value: Any?): Boolean {
    if (value == null) return true
    return omitIdentityFromProto && isIdentity(value)
  }

  /** Returns true if [value] is the identity value of this field's adapter. */
  protected open fun isIdentity(value: Any): Boolean = value == adapter.identity

  /** A present message is always written, even a wrapper that holds an identity value. */
  private val omitIdentityFromProto: Boolean by lazy {
    label == WireField.Label.OMIT_IDENTITY && !writeIdentityValues && !isMessage
  }

  fun omitFromJson(syntax: Syntax, value: Any?): Boolean {
    if (value == null) return true
    return omitIdentity(syntax) && (value == adapter.identity || value.isEmptyPrimitiveArray())
  }

  private fun omitIdentity(syntax: Syntax): Boolean {
    if (writeIdentityValues) return false
    if (label == WireField.Label.OMIT_IDENTITY) return true
    if (label.isRepeated && syntax == Syntax.PROTO_3) return true
    if (isMap && syntax == Syntax.PROTO_3) return true
    return false
  }
}
