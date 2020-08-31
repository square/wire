/*
 * Copyright (C) 2020 Square, Inc.
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
@file:JvmName("JvmLanguages")
package com.squareup.wire.schema.internal

import com.squareup.wire.ProtoAdapter
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Extend
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Options
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import java.lang.annotation.ElementType
import java.math.BigInteger
import java.util.Locale

/*
 * This file contains logic common to code generators targeting the JVM: Kotlin and Java. It's
 * useful to keep these consistent because sometimes a schema will be generated with some parts in
 * one language and other parts in another language.
 */

fun builtInAdapterString(type: ProtoType): String? {
  if (type.isScalar) {
    return "${ProtoAdapter::class.java.name}#${type.toString().toUpperCase(Locale.US)}"
  }
  return when (type) {
    ProtoType.DURATION -> ProtoAdapter::class.java.name + "#DURATION"
    ProtoType.TIMESTAMP -> ProtoAdapter::class.java.name + "#INSTANT"
    ProtoType.EMPTY -> ProtoAdapter::class.java.name + "#EMPTY"
    ProtoType.STRUCT_MAP -> ProtoAdapter::class.java.name + "#STRUCT_MAP"
    ProtoType.STRUCT_VALUE -> ProtoAdapter::class.java.name + "#STRUCT_VALUE"
    ProtoType.STRUCT_NULL -> ProtoAdapter::class.java.name + "#STRUCT_NULL"
    ProtoType.STRUCT_LIST -> ProtoAdapter::class.java.name + "#STRUCT_LIST"
    ProtoType.DOUBLE_VALUE -> ProtoAdapter::class.java.name + "#DOUBLE_VALUE"
    ProtoType.FLOAT_VALUE -> ProtoAdapter::class.java.name + "#FLOAT_VALUE"
    ProtoType.INT64_VALUE -> ProtoAdapter::class.java.name + "#INT64_VALUE"
    ProtoType.UINT64_VALUE -> ProtoAdapter::class.java.name + "#UINT64_VALUE"
    ProtoType.INT32_VALUE -> ProtoAdapter::class.java.name + "#INT32_VALUE"
    ProtoType.UINT32_VALUE -> ProtoAdapter::class.java.name + "#UINT32_VALUE"
    ProtoType.BOOL_VALUE -> ProtoAdapter::class.java.name + "#BOOL_VALUE"
    ProtoType.STRING_VALUE -> ProtoAdapter::class.java.name + "#STRING_VALUE"
    ProtoType.BYTES_VALUE -> ProtoAdapter::class.java.name + "#BYTES_VALUE"
    else -> null
  }
}

fun eligibleAsAnnotationMember(schema: Schema, field: Field): Boolean {
  val type = field.type!!

  if (!type.isScalar && schema.getType(type) !is EnumType) {
    return false
  }

  if (field.packageName == "google.protobuf" || field.packageName == "wire") {
    return false // Don't emit annotations for packed, since, etc.
  }

  if (field.name == "redacted") {
    return false // Redacted is built-in.
  }

  return true
}

fun annotationTargetType(extend: Extend): ElementType? {
  return when (extend.type!!) {
    Options.MESSAGE_OPTIONS, Options.ENUM_OPTIONS, Options.SERVICE_OPTIONS  -> ElementType.TYPE
    Options.FIELD_OPTIONS, Options.ENUM_VALUE_OPTIONS -> ElementType.FIELD
    Options.METHOD_OPTIONS -> ElementType.METHOD
    else -> null
  }
}

fun optionValueToInt(value: Any?): Int {
  if (value == null) return 0

  val string = value.toString()

  return when {
    // Hexadecimal.
    string.startsWith("0x") || string.startsWith("0X") -> string.substring("0x".length).toInt(16)

    // Octal.
    string.startsWith("0") && string != "0" -> error("Octal literal unsupported: $value")

    // Decimal.
    else -> BigInteger(string).toInt()
  }
}

fun optionValueToLong(value: Any?): Long {
  if (value == null) return 0L

  val string = value.toString()

  return when {
    // Hexadecimal.
    string.startsWith("0x") || string.startsWith("0X") -> string.substring("0x".length).toLong(16)

    // Octal.
    string.startsWith("0") && string != "0" -> error("Octal literal unsupported: $value")

    // Decimal.
    else -> BigInteger(string).toLong()
  }
}

fun javaPackage(protoFile: ProtoFile): String {
  val wirePackage = protoFile.wirePackage()
  if (wirePackage != null) return wirePackage

  val javaPackage = protoFile.javaPackage()
  if (javaPackage != null) return javaPackage

  return protoFile.packageName ?: ""
}
