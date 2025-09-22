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

import com.squareup.wire.internal.Serializable
import kotlin.jvm.JvmField

class GrpcStatus private constructor(
  val name: String,
  val code: Int,
) : Serializable {
  init {
    require(code >= 0)
  }

  override fun equals(other: Any?): Boolean = other is GrpcStatus && other.code == code

  override fun hashCode(): Int = code

  companion object {
    @JvmField val OK: GrpcStatus = GrpcStatus("OK", 0)

    @JvmField val CANCELLED: GrpcStatus = GrpcStatus("CANCELLED", 1)

    @JvmField val UNKNOWN: GrpcStatus = GrpcStatus("UNKNOWN", 2)

    @JvmField val INVALID_ARGUMENT: GrpcStatus = GrpcStatus("INVALID_ARGUMENT", 3)

    @JvmField val DEADLINE_EXCEEDED: GrpcStatus = GrpcStatus("DEADLINE_EXCEEDED", 4)

    @JvmField val NOT_FOUND: GrpcStatus = GrpcStatus("NOT_FOUND", 5)

    @JvmField val ALREADY_EXISTS: GrpcStatus = GrpcStatus("ALREADY_EXISTS", 6)

    @JvmField val PERMISSION_DENIED: GrpcStatus = GrpcStatus("PERMISSION_DENIED", 7)

    @JvmField val RESOURCE_EXHAUSTED: GrpcStatus = GrpcStatus("RESOURCE_EXHAUSTED", 8)

    @JvmField val FAILED_PRECONDITION: GrpcStatus = GrpcStatus("FAILED_PRECONDITION", 9)

    @JvmField val ABORTED: GrpcStatus = GrpcStatus("ABORTED", 10)

    @JvmField val OUT_OF_RANGE: GrpcStatus = GrpcStatus("OUT_OF_RANGE", 11)

    @JvmField val UNIMPLEMENTED: GrpcStatus = GrpcStatus("UNIMPLEMENTED", 12)

    @JvmField val INTERNAL: GrpcStatus = GrpcStatus("INTERNAL", 13)

    @JvmField val UNAVAILABLE: GrpcStatus = GrpcStatus("UNAVAILABLE", 14)

    @JvmField val DATA_LOSS: GrpcStatus = GrpcStatus("DATA_LOSS", 15)

    @JvmField val UNAUTHENTICATED: GrpcStatus = GrpcStatus("UNAUTHENTICATED", 16)

    private val INSTANCES = listOf(
      OK,
      CANCELLED,
      UNKNOWN,
      INVALID_ARGUMENT,
      DEADLINE_EXCEEDED,
      NOT_FOUND,
      ALREADY_EXISTS,
      PERMISSION_DENIED,
      RESOURCE_EXHAUSTED,
      FAILED_PRECONDITION,
      ABORTED,
      OUT_OF_RANGE,
      UNIMPLEMENTED,
      INTERNAL,
      UNAVAILABLE,
      DATA_LOSS,
      UNAUTHENTICATED,
    ).also {
      for ((index, grpcStatus) in it.withIndex()) {
        check(index == grpcStatus.code)
      }
    }

    /**
     * Returns an instance with a well-known name (like "OK"), or a string like "STATUS_99" if the
     * code was not known when this was built.
     */
    fun get(status: Int): GrpcStatus =
      INSTANCES.getOrNull(status) ?: GrpcStatus("STATUS_$status", status)
  }
}
