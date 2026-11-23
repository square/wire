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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.schema.internal.GOOGLE_RPC_ERROR_DETAILS_PACKAGE
import com.squareup.wire.schema.internal.GOOGLE_RPC_ERROR_DETAILS_TYPES
import kotlin.test.Test

/**
 * Checks that the hard-coded [GOOGLE_RPC_ERROR_DETAILS_TYPES] matches the bundled
 * `google/rpc/error_details.proto` and the runtime classes it names.
 */
class GoogleRpcErrorDetailsTypesTest {
  @Test fun mapCoversEveryTypeOfTheBundledProto() {
    val protoFile = CoreLoader.load("google/rpc/error_details.proto")
    val bundledTypes = mutableSetOf<ProtoType>()
    fun collect(types: List<Type>) {
      for (type in types) {
        bundledTypes += type.type
        collect(type.nestedTypes)
      }
    }
    collect(protoFile.types)

    assertThat(GOOGLE_RPC_ERROR_DETAILS_TYPES.keys).isEqualTo(bundledTypes)
  }

  @Test fun mapNamesRuntimeClassesWithMatchingTypeUrls() {
    for ((protoType, names) in GOOGLE_RPC_ERROR_DETAILS_TYPES) {
      val className = "$GOOGLE_RPC_ERROR_DETAILS_PACKAGE.${names.joinToString("$")}"
      val companion = Class.forName(className).getField("Companion").get(null)
      val adapter = companion.javaClass.getMethod("getADAPTER").invoke(companion) as ProtoAdapter<*>
      assertThat(adapter.typeUrl, name = className)
        .isEqualTo("type.googleapis.com/$protoType")
    }
  }
}
