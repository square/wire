/*
 * Copyright 2013 Square Inc.
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
package com.squareup.wire

import com.squareup.wire.WireField.Label
import kotlin.jvm.JvmName

/**
 * Annotates generated [Message] fields with metadata for serialization and deserialization.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WireField(
  /** The tag number used to store the field's value. */
  val tag: Int,
  /**
   * Reference to the static field that holds a [ProtoAdapter] that can encode and decode this
   * field's keys. This only applies to maps. See [adapter] for a description of the value format.
   */
  val keyAdapter: String = "",
  /**
   * Reference to the static field that holds a [ProtoAdapter] that can encode and decode this
   * field's values. The reference is a string like `com.squareup.wire.protos.person.Person#ADAPTER`
   * and contains a fully-qualified class name followed by a hash symbol and a field name.
   */
  val adapter: String,
  /**
   * The field's protocol buffer label, one of [Label.OPTIONAL], [Label.REQUIRED], [Label.REPEATED],
   * or [Label.PACKED]. Defaults to [Label.OPTIONAL].
   */
  val label: Label = Label.OPTIONAL,
  /**
   * Redacted fields are omitted from toString() to protect sensitive data. Defaults to false.
   */
  val redacted: Boolean = false,
  /**
   * Name of this field as declared in the proto schema. This value is set to a non-empty string
   * only when the declared name differs from the generated one; for instance, a proto field named
   * `final` generated in Java will be renamed to `final_`.
   */
  val declaredName: String = "",
  /**
   * Name representing this field as it should be used in JSON. This value is set to a non-empty
   * string only when the json name differs from the name as declared in the proto schema.
   */
  val jsonName: String = "",
  /**
   * The field's encode mode, one of [EncodeModel.NULL_IF_ABSENT], [EncodeModel.REQUIRED],
   * [EncodeModel.IDENTITY_IF_ABSENT], [EncodeModel.REPEATED], [EncodeModel.PACKED], or
   * [EncodeModel.MAP]. Defaults to [EncodeModel.NULL_IF_ABSENT].
   */
  val encodeMode: EncodeMode = EncodeMode.NULL_IF_ABSENT
) {

  /** A protocol buffer label.  */
  enum class Label {
    REQUIRED,
    OPTIONAL,
    REPEATED,
    ONE_OF,
    /** Implies [REPEATED]. */
    PACKED;

    val isRepeated: Boolean
      @JvmName("isRepeated") get() = this == REPEATED || this == PACKED

    val isPacked: Boolean
      @JvmName("isPacked") get() = this == PACKED

    val isOneOf: Boolean
      @JvmName("isOneOf") get() = this == ONE_OF
  }

  /** Sets how a member should be treated on encoding, and decoding. */
  enum class EncodeMode {
    /** Absence is treated as the equivalent of a null value. */
    NULL_IF_ABSENT,
    /** Absence isn't allowed. */
    THROW_IF_ABSENT,
    /**
     * Absence is treated as the equivalent to the field's type identity values.
     * e.g: `0` for `int32` */
    IDENTITY_IF_ABSENT,
    /** List. */
    REPEATED,
    /** Packed encoded list. */
    PACKED,
    /** Map. */
    MAP,
  }
}
