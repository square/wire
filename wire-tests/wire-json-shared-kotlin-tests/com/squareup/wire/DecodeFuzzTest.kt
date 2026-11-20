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

import com.squareup.wire.proto2.alltypes.AllTypes as AllTypesProto2
import com.squareup.wire.proto3.alltypes.AllTypes as AllTypesProto3
import com.squareup.wire.testing.DecodeFuzzer
import com.squareup.wire.testing.DecodeFuzzer.Case
import com.squareup.wire.testing.DecodeFuzzer.Config
import kotlin.test.Test
import squareup.proto3.All32
import squareup.proto3.All64
import squareup.proto3.AllStructs
import squareup.proto3.AllWrappers
import squareup.proto3.CamelCase
import squareup.proto3.ContainsFieldMask
import squareup.proto3.Envelope
import squareup.proto3.MapTypes
import squareup.proto3.PizzaDelivery

/**
 * Decodes random wire bytes with the adapters of the JSON test modules. These include messages
 * that hold `Any`, `Duration`, `Timestamp`, `Struct`, `FieldMask` and the wrapper types.
 * This file runs against Java adapters that read their fields by reflection, and against generated
 * Kotlin adapters. See [DecodeFuzzer] for the contract and for how to run a longer local fuzz.
 */
class DecodeFuzzTest {
  @Test fun decodingRandomBytesThrowsOnlyIOException() {
    DecodeFuzzer.fuzzAll(
      listOf(
        // The proto2 AllTypes has required fields, so the fuzzer mutates a valid message.
        Case(
          "AllTypes proto2",
          AllTypesProto2.ADAPTER,
          Config(
            seeds = listOf(DecodeFuzzer.allTypesRequiredFields),
            tags = (1001..1020).toList(),
            missingRequiredFieldsAllowed = true,
          ),
        ),
        Case("AllTypes proto3", AllTypesProto3.ADAPTER),
        Case("PizzaDelivery", PizzaDelivery.ADAPTER),
        Case("AllStructs", AllStructs.ADAPTER),
        Case("AllWrappers", AllWrappers.ADAPTER),
        Case("Envelope", Envelope.ADAPTER),
        Case("MapTypes", MapTypes.ADAPTER),
        Case("ContainsFieldMask", ContainsFieldMask.ADAPTER),
        Case("CamelCase", CamelCase.ADAPTER),
        Case("All32", All32.ADAPTER),
        Case("All64", All64.ADAPTER),
      ),
    )
  }
}
