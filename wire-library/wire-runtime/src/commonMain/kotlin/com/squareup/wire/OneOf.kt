/*
 * Copyright 2020 Square Inc.
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

import com.squareup.wire.internal.sanitize

/**
 * A oneof's value and the key that identifies which field holds it.
 *
 * Wire has two different mechanisms for generating `oneof` code: “boxed” that uses this class,
 * and “flat” where all oneof fields are defined inline in the declaring message.
 *
 * Flat oneofs
 * -----------
 *
 * This is Wire's default mechanism. Each oneof field in the schema yields a field in the generated
 * Kotlin or Java class. At most one field has a non-null value, and callers need to manually probe
 * each field until they find the non-null value.
 *
 * This is well-suited to oneofs with a small number of choices (less than 5).
 *
 * Boxed oneofs
 * ------------
 *
 * In this mechanism the generated class has one field that holds an instance of this. It has both
 * a key that identifies which field is populated, and its value. If no field is set, then the
 * OneOf box is null.
 *
 * This is well-suited to oneofs with a large number of choices (5 or more). This mechanism is
 * necessary for oneofs with a very large number of options because in the other form the
 * generated code may exceed the JVM's method size limits.
 *
 * Opt-in to boxed oneofs in your build by setting the `boxOneOfsMinSize` option. OneOfs with this
 * many fields or more will be generated in this form.
 *
 * ```
 * wire {
 *   kotlin {
 *     boxOneOfsMinSize = 10
 *   }
 * }
 * ```
 *
 * Using Boxed oneofs
 * ------------------
 *
 * One challenge in using oneofs is coping with data from different schema versions that offer
 * new unknown choices. For example, a client may receive a message from a server that sets a
 * oneof field the client doesn't know about; or a server may read an archived message with a
 * oneof field that has since been deleted.
 *
 * In either case, the oneof will be null! There is no way to differentiate between unset and set
 * to an unknown field. Please keep this in mind when writing code to handle oneofs.
 *
 * In this example the address type may be absent (no value was ever set) or its type may be too
 * new for the current code to understand. This code returns a default value:
 *
 * ```
 * val buttonLabel = when (contact.address?.key) {
 *   Contact.addressSmsNumber -> "Send SMS"
 *   Contact.addressEmailAddress -> "Send Email"
 *   else -> "Address Type Unknown or Unset"
 * }
 * ```
 *
 * Another approach is to crash. In this case applications need to be careful to avoid receiving
 * data with an incompatible schema.
 *
 * ```
 * val buttonLabel = when (contact.address?.key) {
 *   Contact.addressSmsNumber -> "Send SMS"
 *   Contact.addressEmailAddress -> "Send Email"
 *   else -> throw IllegalStateException("unknown address!")
 * }
 * ```
 *
 * Consider AnyMessage
 * -------------------
 *
 * New schemas should consider [google.protobuf.Any][AnyMessage] instead of `oneof`.
 *
 * Benefits of `Any`:
 *
 *  * No build-time dependency from the referencing type on the referenced type.
 *  * Add new types without changing the schema
 *
 * Benefits of `OneOf`:
 *
 *  * More compact on-the-wire encoding. Approximately 1 byte of overhead for `OneOf` vs. `32` for
 *    the message name in an `Any`.
 *  * All choices are cataloged a central place in the schema.
 */
data class OneOf<out K : OneOf.Key<T>, T>(
  val key: K,
  val value: T
) {
  @Suppress("UNCHECKED_CAST") // We statically guarantee Keys and Values are of the same types.
  fun <X> getOrNull(key: Key<X>) : X? {
    if (this.key == key) return value as X
    return null
  }

  override fun toString(): String {
    // TODO(Benoit) What if the field is redacted?
    val valueAsString = when (key.adapter) {
      ProtoAdapter.STRING, ProtoAdapter.STRING_VALUE -> {
        sanitize(value.toString())
      }
      else -> {
        value.toString()
      }
    }

    return """${key.declaredName}=$valueAsString"""
  }

  fun encodedSizeWithTag(): Int {
    return key.adapter.encodedSizeWithTag(key.tag, value)
  }

  fun encodeWithTag(writer: ProtoWriter) {
    key.adapter.encodeWithTag(writer, key.tag, value)
  }

  fun encodeWithTag(writer: ReverseProtoWriter) {
    key.adapter.encodeWithTag(writer, key.tag, value)
  }

  /**
   * Identifies a field in a OneOf. Typically subclasses are generated by the Wire compiler and
   * instances are declared as members of the referencing message class.
   *
   * Note that the field's type may not uniquely identify the field. For example:
   *
   * ```
   * message ContactAddress {
   *   string name = 1;
   *
   *   oneof address {
   *     string sms_number = 2;
   *     string email_address = 3;
   *   }
   * }
   * ```
   */
  abstract class Key<T>(
    val tag: Int,
    val adapter: ProtoAdapter<T>,
    val declaredName: String,
    val redacted: Boolean = false,
    val jsonName: String = ""
  )
}
