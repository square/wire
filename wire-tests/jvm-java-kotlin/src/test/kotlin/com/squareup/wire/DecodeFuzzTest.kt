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

import com.squareup.wire.map.Mappy
import com.squareup.wire.map.MappyTwo
import com.squareup.wire.protos.alltypes.AllTypes
import com.squareup.wire.protos.edgecases.MergeChoice
import com.squareup.wire.protos.edgecases.Recursive
import com.squareup.wire.protos.empty_packed.EmptyPacked
import com.squareup.wire.protos.oneof.OneOfMessage
import com.squareup.wire.protos.person.Person
import com.squareup.wire.protos.unknownfields.VersionTwo
import com.squareup.wire.testing.DecodeFuzzer
import com.squareup.wire.testing.DecodeFuzzer.Case
import com.squareup.wire.testing.DecodeFuzzer.Config
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import squareup.protos.packed_encoding.OuterMessage

/**
 * Decodes random wire bytes with generated Java adapters, and with the reflective adapters that
 * `ProtoAdapter.newMessageAdapter()` builds for the same classes.
 * See [DecodeFuzzer] for the contract and for how to run a longer local fuzz.
 */
class DecodeFuzzTest {
  @Test fun decodingRandomBytesThrowsOnlyIOException() {
    // AllTypes and Person have required fields, so the fuzzer mutates valid messages.
    val allTypes = Config(
      seeds = listOf(TestAllTypesData.expectedOutput, DecodeFuzzer.allTypesRequiredFields),
      tags = (1001..1020).toList(),
      missingRequiredFieldsAllowed = true,
    )
    val person = Config(seeds = listOf("0a01611001".decodeHex()), missingRequiredFieldsAllowed = true)

    DecodeFuzzer.fuzzAll(
      listOf(
        Case("AllTypes", AllTypes.ADAPTER, allTypes),
        Case("reflective AllTypes", ProtoAdapter.newMessageAdapter(AllTypes::class.java), allTypes),
        Case("Person", Person.ADAPTER, person),
        Case("reflective Person", ProtoAdapter.newMessageAdapter(Person::class.java), person),
        Case("OneOfMessage", OneOfMessage.ADAPTER),
        Case("reflective OneOfMessage", ProtoAdapter.newMessageAdapter(OneOfMessage::class.java)),
        Case("MergeChoice", MergeChoice.ADAPTER),
        Case("reflective MergeChoice", ProtoAdapter.newMessageAdapter(MergeChoice::class.java)),
        Case("Recursive", Recursive.ADAPTER),
        Case("reflective Recursive", ProtoAdapter.newMessageAdapter(Recursive::class.java)),
        Case("EmptyPacked", EmptyPacked.ADAPTER),
        Case("Mappy", Mappy.ADAPTER),
        Case("reflective Mappy", ProtoAdapter.newMessageAdapter(Mappy::class.java)),
        Case("MappyTwo", MappyTwo.ADAPTER),
        Case("ModelEvaluation", ModelEvaluation.ADAPTER),
        Case("OuterMessage", OuterMessage.ADAPTER),
        Case("VersionTwo", VersionTwo.ADAPTER),
        Case("reflective VersionTwo", ProtoAdapter.newMessageAdapter(VersionTwo::class.java)),
      ),
    )
  }
}
