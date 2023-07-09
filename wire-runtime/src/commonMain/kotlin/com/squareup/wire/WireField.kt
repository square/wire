/*
 * Copyright (C) 2013 Square, Inc.
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
   * [Label.PACKED], or [Label.OMIT_IDENTITY]. Defaults to [Label.OPTIONAL].
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
   * Name of the oneof this field belongs to. This value is set to a non-empty string only if the
   * field is part of a oneof.
   */
  val oneofName: String = "",
  /**
   * This is the order that this field was declared in the `.proto` schema.
   *
   * It is -1 if the order does not matter for JSON serialization.
   */
  val schemaIndex: Int = -1,
) {

  /** A protocol buffer label.  */
  enum class Label {
    REQUIRED,
    OPTIONAL,
    REPEATED,
    ONE_OF,

    /** Implies [REPEATED]. */
    PACKED,

    /**
     * Special label to define proto3 fields which should not be emitted if their value is equal
     * to their type's respective identity value. E.g.: a field of type `int32` will not get emitted
     * if its value is `0`.
     */
    OMIT_IDENTITY,
    ;

    val isRepeated: Boolean
      @JvmName("isRepeated")
      get() = this == REPEATED || this == PACKED

    val isPacked: Boolean
      @JvmName("isPacked")
      get() = this == PACKED

    val isOneOf: Boolean
      @JvmName("isOneOf")
      get() = this == ONE_OF
  }
}
