/*
 * Copyright (C) 2020 Square, Inc.
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

import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString.copyFrom
import com.google.protobuf.BytesValue
import com.google.protobuf.DoubleValue
import com.google.protobuf.FloatValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.google.protobuf.UInt32Value
import com.google.protobuf.UInt64Value
import okio.ByteString

internal fun Double.toDoubleValue(): DoubleValue =
  DoubleValue.newBuilder().setValue(this).build()

internal fun Float.toFloatValue(): FloatValue =
  FloatValue.newBuilder().setValue(this).build()

internal fun Long.toInt64Value(): Int64Value =
  Int64Value.newBuilder().setValue(this).build()

internal fun Long.toUInt64Value(): UInt64Value =
  UInt64Value.newBuilder().setValue(this).build()

internal fun Int.toInt32Value(): Int32Value =
  Int32Value.newBuilder().setValue(this).build()

internal fun Int.toUInt32Value(): UInt32Value =
  UInt32Value.newBuilder().setValue(this).build()

internal fun Boolean.toBoolValue(): BoolValue =
  BoolValue.newBuilder().setValue(this).build()

internal fun String.toStringValue(): StringValue =
  StringValue.newBuilder().setValue(this).build()

internal fun ByteString.toBytesValue(): BytesValue =
  BytesValue.newBuilder().setValue(copyFrom(this.toByteArray())).build()
