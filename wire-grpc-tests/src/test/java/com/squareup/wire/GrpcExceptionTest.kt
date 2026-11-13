/*
 * Copyright (C) 2023 Square, Inc.
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
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import org.junit.Test

class GrpcExceptionTest {

  @Test(expected = Test.None::class)
  fun serialization() {
    val grpcException =
      GrpcException(
        grpcStatus = GrpcStatus.FAILED_PRECONDITION,
        grpcMessage = "message",
        grpcStatusDetails = byteArrayOf(1, 2, 3),
      )
    ObjectOutputStream(ByteArrayOutputStream()).use { os -> os.writeObject(grpcException) }
  }

  @Test
  fun createFromRpcStatus() {
    val rpcStatus = RpcStatus(
      code = GrpcStatus.FAILED_PRECONDITION.code,
      message = "message",
      details = listOf(AnyMessage("type.googleapis.com/x.Y")),
    )
    val grpcException = GrpcException(rpcStatus)
    assertThat(grpcException.grpcStatus).isEqualTo(GrpcStatus.FAILED_PRECONDITION)
    assertThat(grpcException.grpcMessage).isEqualTo("message")
    assertThat(grpcException.rpcStatus).isEqualTo(rpcStatus)
  }

  @Test
  fun rpcStatusIsNullWithoutDetails() {
    val grpcException = GrpcException(GrpcStatus.INTERNAL, "message")
    assertThat(grpcException.rpcStatus).isNull()
  }

  @Test
  fun rpcStatusIsNullForInvalidDetails() {
    // A length-delimited field 1 whose length runs past the end of the bytes.
    val grpcException = GrpcException(GrpcStatus.INTERNAL, "message", byteArrayOf(0x0a, 0x7f))
    assertThat(grpcException.rpcStatus).isNull()
  }
}
