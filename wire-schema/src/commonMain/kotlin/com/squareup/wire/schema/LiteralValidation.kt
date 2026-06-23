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
package com.squareup.wire.schema

internal fun isValidLiteral(linker: Linker, type: ProtoType, value: String): Boolean = when (type) {
  ProtoType.BOOL -> value == "true" || value == "false"
  ProtoType.BYTES, ProtoType.STRING -> true
  ProtoType.DOUBLE, ProtoType.FLOAT -> value.isValidFloatingPointDefault()
  ProtoType.FIXED32, ProtoType.UINT32 -> value.isValidUnsignedIntegerDefault(UINT32_MAX)
  ProtoType.FIXED64, ProtoType.UINT64 -> value.isValidUnsignedIntegerDefault(UINT64_MAX)
  ProtoType.INT32, ProtoType.SFIXED32, ProtoType.SINT32 -> {
    value.isValidSignedIntegerDefault(INT32_MIN, INT32_MAX)
  }
  ProtoType.INT64, ProtoType.SFIXED64, ProtoType.SINT64 -> {
    value.isValidSignedIntegerDefault(INT64_MIN, INT64_MAX)
  }
  else -> {
    val valueType = linker.get(type)
    valueType is EnumType && valueType.constant(value) != null
  }
}

private const val INT32_MAX = "2147483647"
private const val INT32_MIN = "-2147483648"
private const val UINT32_MAX = "4294967295"
private const val INT64_MAX = "9223372036854775807"
private const val INT64_MIN = "-9223372036854775808"
private const val UINT64_MAX = "18446744073709551615"

private val DECIMAL_INTEGER_REGEX = Regex("-?[0-9]+")
private val FLOATING_POINT_REGEX = Regex("-?((([0-9]+)(\\.[0-9]*)?)|(\\.[0-9]+))([eE][+-]?[0-9]+)?")
private val HEX_INTEGER_REGEX = Regex("-?0[xX][0-9a-fA-F]+")

private fun String.isValidFloatingPointDefault(): Boolean {
  if (this in listOf("inf", "-inf", "nan", "-nan")) return true
  return FLOATING_POINT_REGEX.matches(this) && toDoubleOrNull() != null
}

private fun String.isValidSignedIntegerDefault(min: String, max: String): Boolean {
  if (!isIntegerLiteral() || hasOctalPrefix()) return false
  return compareIntegerLiterals(min, this) <= 0 && compareIntegerLiterals(this, max) <= 0
}

private fun String.isValidUnsignedIntegerDefault(max: String): Boolean {
  if (!isIntegerLiteral() || hasOctalPrefix() || startsWith("-")) return false
  return compareIntegerLiterals(this, max) <= 0
}

private fun String.isIntegerLiteral() = DECIMAL_INTEGER_REGEX.matches(this) || HEX_INTEGER_REGEX.matches(this)

private fun String.hasOctalPrefix(): Boolean {
  val digits = removePrefix("-")
  return digits.length > 1 && digits[0] == '0' && digits[1] != 'x' && digits[1] != 'X'
}

private fun compareIntegerLiterals(a: String, b: String): Int {
  val aNegative = a.startsWith("-")
  val bNegative = b.startsWith("-")
  if (aNegative != bNegative) return if (aNegative) -1 else 1

  val magnitudeComparison = compareIntegerMagnitudes(
    a.removePrefix("-"),
    b.removePrefix("-"),
  )
  return if (aNegative) -magnitudeComparison else magnitudeComparison
}

private fun compareIntegerMagnitudes(a: String, b: String): Int {
  val aDecimal = a.decimalMagnitude()
  val bDecimal = b.decimalMagnitude()
  if (aDecimal.length != bDecimal.length) return aDecimal.length.compareTo(bDecimal.length)
  return aDecimal.compareTo(bDecimal)
}

private fun String.decimalMagnitude(): String {
  val stripped = if (startsWith("0x") || startsWith("0X")) {
    hexMagnitudeToDecimal(substring(2))
  } else {
    trimStart('0').ifEmpty { "0" }
  }
  return stripped.trimStart('0').ifEmpty { "0" }
}

private fun hexMagnitudeToDecimal(hex: String): String {
  var decimal = "0"
  hex.forEach { digit ->
    decimal = decimal.multiplyDecimalBy(16)
    decimal = decimal.addDecimal(digit.digitToInt(16))
  }
  return decimal
}

private fun String.multiplyDecimalBy(multiplier: Int): String {
  var carry = 0
  val result = StringBuilder(length + 2)
  for (i in indices.reversed()) {
    val value = (this[i] - '0') * multiplier + carry
    result.append(value % 10)
    carry = value / 10
  }
  while (carry > 0) {
    result.append(carry % 10)
    carry /= 10
  }
  return result.reverse().toString().trimStart('0').ifEmpty { "0" }
}

private fun String.addDecimal(addend: Int): String {
  var carry = addend
  val result = StringBuilder(length + 2)
  for (i in indices.reversed()) {
    val value = (this[i] - '0') + carry
    result.append(value % 10)
    carry = value / 10
  }
  while (carry > 0) {
    result.append(carry % 10)
    carry /= 10
  }
  return result.reverse().toString().trimStart('0').ifEmpty { "0" }
}
