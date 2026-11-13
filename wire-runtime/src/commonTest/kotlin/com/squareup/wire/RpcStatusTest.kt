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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex

class RpcStatusTest {
  private val status = RpcStatus(
    code = 5,
    message = "nope",
    details = listOf(AnyMessage("type.googleapis.com/x.Y", "0801".decodeHex())),
  )

  @Test fun encodesLikeGoogleRpcStatus() {
    assertThat(RpcStatus.ADAPTER.encodeByteString(status))
      .isEqualTo("080512046e6f70651a1d0a17747970652e676f6f676c65617069732e636f6d2f782e5912020801".decodeHex())
  }

  @Test fun roundTrips() {
    assertThat(RpcStatus.ADAPTER.decode(status.encode())).isEqualTo(status)
  }

  @Test fun typeUrlIsGoogleRpcStatus() {
    assertThat(RpcStatus.ADAPTER.typeUrl).isEqualTo("type.googleapis.com/google.rpc.Status")
  }

  @Test fun packsIntoAny() {
    val any = AnyMessage.pack(status)
    assertThat(any.typeUrl).isEqualTo("type.googleapis.com/google.rpc.Status")
    assertThat(any.unpack(RpcStatus.ADAPTER)).isEqualTo(status)
  }

  @Test fun detailsAreImmutableCopy() {
    val details = mutableListOf(AnyMessage("type.googleapis.com/x.Y"))
    val status = RpcStatus(details = details)
    details += AnyMessage("type.googleapis.com/x.Z")
    assertThat(status.details).containsExactly(AnyMessage("type.googleapis.com/x.Y"))
  }
}
