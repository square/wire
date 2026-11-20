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
package com.squareup.wire.rpc

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.squareup.wire.AnyMessage
import com.squareup.wire.RpcStatus
import com.squareup.wire.durationOfSeconds
import kotlin.test.Test

class ErrorDetailsTest {
  @Test fun typeUrlsAreGoogleRpc() {
    val typeUrls = listOf(
      BadRequest.ADAPTER,
      BadRequest.FieldViolation.ADAPTER,
      DebugInfo.ADAPTER,
      ErrorInfo.ADAPTER,
      Help.ADAPTER,
      Help.Link.ADAPTER,
      LocalizedMessage.ADAPTER,
      PreconditionFailure.ADAPTER,
      PreconditionFailure.Violation.ADAPTER,
      QuotaFailure.ADAPTER,
      QuotaFailure.Violation.ADAPTER,
      RequestInfo.ADAPTER,
      ResourceInfo.ADAPTER,
      RetryInfo.ADAPTER,
    ).map { it.typeUrl }
    assertThat(typeUrls).containsExactly(
      "type.googleapis.com/google.rpc.BadRequest",
      "type.googleapis.com/google.rpc.BadRequest.FieldViolation",
      "type.googleapis.com/google.rpc.DebugInfo",
      "type.googleapis.com/google.rpc.ErrorInfo",
      "type.googleapis.com/google.rpc.Help",
      "type.googleapis.com/google.rpc.Help.Link",
      "type.googleapis.com/google.rpc.LocalizedMessage",
      "type.googleapis.com/google.rpc.PreconditionFailure",
      "type.googleapis.com/google.rpc.PreconditionFailure.Violation",
      "type.googleapis.com/google.rpc.QuotaFailure",
      "type.googleapis.com/google.rpc.QuotaFailure.Violation",
      "type.googleapis.com/google.rpc.RequestInfo",
      "type.googleapis.com/google.rpc.ResourceInfo",
      "type.googleapis.com/google.rpc.RetryInfo",
    )
  }

  @Test fun unpackDetailsFromRpcStatus() {
    val errorInfo = ErrorInfo(reason = "NO_TREES", domain = "routeguide", metadata = mapOf("k" to "v"))
    val retryInfo = RetryInfo(retry_delay = durationOfSeconds(3, 0))
    val status = RpcStatus(
      code = 9,
      message = "boom",
      details = listOf(AnyMessage.pack(errorInfo), AnyMessage.pack(retryInfo)),
    )

    val decoded = RpcStatus.ADAPTER.decode(status.encode())
    assertThat(decoded.details.firstNotNullOf { it.unpackOrNull(ErrorInfo.ADAPTER) }).isEqualTo(errorInfo)
    assertThat(decoded.details.firstNotNullOf { it.unpackOrNull(RetryInfo.ADAPTER) }).isEqualTo(retryInfo)
  }
}
