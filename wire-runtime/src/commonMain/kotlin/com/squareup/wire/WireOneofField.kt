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
package com.squareup.wire

/**
 * Annotates generated data classes inside a sealed-class oneof with metadata for serialization and
 * deserialization. Each variant data class carries its own tag, adapter, and name metadata,
 * discovered at runtime via reflection on the sealed class's nested classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WireOneofField(
  /** The tag number used to store the field's value. */
  val tag: Int,
  /**
   * Reference to the static field that holds a [ProtoAdapter] that can encode and decode this
   * field's values. The reference is a string like `com.squareup.wire.protos.person.Person#ADAPTER`
   * and contains a fully-qualified class name followed by a hash symbol and a field name.
   */
  val adapter: String,
  /**
   * Name of this field as declared in the proto schema. This value is set to a non-empty string
   * only when the declared name differs from the generated one; for instance, a proto field named
   * `final` generated in Java will be renamed to `final_`.
   */
  val declaredName: String = "",
  /**
   * Redacted fields are omitted from toString() to protect sensitive data. Defaults to false.
   */
  val redacted: Boolean = false,
  /**
   * Name representing this field as it should be used in JSON. This value is set to a non-empty
   * string only when the json name differs from the name as declared in the proto schema.
   */
  val jsonName: String = "",
)
