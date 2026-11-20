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

import com.squareup.wire.protos.kotlin.Form
import com.squareup.wire.protos.kotlin.OneOfMessage
import com.squareup.wire.protos.kotlin.OneOfMessageWithMessages
import com.squareup.wire.protos.kotlin.alltypes.AllTypes
import com.squareup.wire.protos.kotlin.edgecases.MergeChoice
import com.squareup.wire.protos.kotlin.edgecases.Recursive
import com.squareup.wire.protos.kotlin.map.Mappy
import com.squareup.wire.protos.kotlin.map.MappyTwo
import com.squareup.wire.protos.kotlin.oneofmergeboxed.BoxedMerge
import com.squareup.wire.protos.kotlin.oneofmergesealed.SealedMerge
import com.squareup.wire.protos.kotlin.person.Person
import com.squareup.wire.protos.kotlin.repeated.Repeated
import com.squareup.wire.protos.kotlin.unknownfields.VersionTwo
import com.squareup.wire.protos.usesany.UsesAny
import com.squareup.wire.testing.DecodeFuzzer
import com.squareup.wire.testing.DecodeFuzzer.Case
import com.squareup.wire.testing.DecodeFuzzer.Config
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import squareup.protos.packed_encoding.OuterMessage

/**
 * Decodes random wire bytes with the generated Kotlin adapters and the well-known type adapters.
 * See [DecodeFuzzer] for the contract and for how to run a longer local fuzz.
 */
class DecodeFuzzTest {
  @Test fun decodingRandomBytesThrowsOnlyIOException() {
    DecodeFuzzer.fuzzAll(
      listOf(
        // AllTypes has required fields, so the fuzzer mutates a valid message.
        Case(
          "AllTypes",
          AllTypes.ADAPTER,
          Config(
            seeds = listOf(TestAllTypesData.expectedOutput, DecodeFuzzer.allTypesRequiredFields),
            tags = (1001..1020).toList(),
            missingRequiredFieldsAllowed = true,
          ),
        ),
        // Person has required fields too.
        Case(
          "Person",
          Person.ADAPTER,
          Config(seeds = listOf("0a01611001".decodeHex()), missingRequiredFieldsAllowed = true),
        ),
        Case("Form", Form.ADAPTER),
        Case("OneOfMessage", OneOfMessage.ADAPTER),
        Case("OneOfMessageWithMessages", OneOfMessageWithMessages.ADAPTER),
        Case("BoxedMerge", BoxedMerge.ADAPTER),
        Case("SealedMerge", SealedMerge.ADAPTER),
        Case("MergeChoice", MergeChoice.ADAPTER),
        Case("Recursive", Recursive.ADAPTER),
        Case("Mappy", Mappy.ADAPTER),
        Case("MappyTwo", MappyTwo.ADAPTER),
        Case("ModelEvaluation", ModelEvaluation.ADAPTER),
        Case("OuterMessage", OuterMessage.ADAPTER),
        Case("Repeated", Repeated.ADAPTER),
        Case("VersionTwo", VersionTwo.ADAPTER),
        Case("UsesAny", UsesAny.ADAPTER),
        Case("Any", AnyMessage.ADAPTER),
        Case("Duration", ProtoAdapter.DURATION),
        Case("Timestamp", ProtoAdapter.INSTANT),
        Case("Struct", ProtoAdapter.STRUCT_MAP),
        Case("ListValue", ProtoAdapter.STRUCT_LIST),
        Case("Value", ProtoAdapter.STRUCT_VALUE),
        Case("Int64Value", ProtoAdapter.INT64_VALUE),
        Case("StringValue", ProtoAdapter.STRING_VALUE),
      ),
    )
  }
}
