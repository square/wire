/*
 * Copyright (C) 2021 Square, Inc.
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

import okio.IOException

class GrpcException(
  val grpcStatus: GrpcStatus,
  val grpcMessage: String?,
  val grpcStatusDetails: ByteArray? = null,
  val url: String? = null,
) : IOException(
  buildString {
    append("grpc-status=${grpcStatus.code} grpc-status-name=${grpcStatus.name}")
    if (grpcMessage != null) append(" grpc-message=$grpcMessage")
    if (url != null) append(" url=$url")
  },
) {
  @Deprecated(
    message = "added url parameter",
    level = DeprecationLevel.HIDDEN,
  )
  constructor(
    grpcStatus: GrpcStatus,
    grpcMessage: String?,
    grpcStatusDetails: ByteArray? = null,
  ) : this(grpcStatus, grpcMessage, grpcStatusDetails)
}
