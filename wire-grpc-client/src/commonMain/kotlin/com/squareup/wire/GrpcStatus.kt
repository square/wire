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
import kotlin.jvm.Synchronized

class GrpcStatus private constructor(
  val name: String,
  val code: Int,
) : Serializable {
  companion object {
    private val INSTANCES = mutableMapOf<Int, GrpcStatus>()

    @JvmField val OK: GrpcStatus = create("OK", 0)

    @JvmField val CANCELLED: GrpcStatus = create("CANCELLED", 1)

    @JvmField val UNKNOWN: GrpcStatus = create("UNKNOWN", 2)

    @JvmField val INVALID_ARGUMENT: GrpcStatus = create("INVALID_ARGUMENT", 3)

    @JvmField val DEADLINE_EXCEEDED: GrpcStatus = create("DEADLINE_EXCEEDED", 4)

    @JvmField val NOT_FOUND: GrpcStatus = create("NOT_FOUND", 5)

    @JvmField val ALREADY_EXISTS: GrpcStatus = create("ALREADY_EXISTS", 6)

    @JvmField val PERMISSION_DENIED: GrpcStatus = create("PERMISSION_DENIED", 7)

    @JvmField val RESOURCE_EXHAUSTED: GrpcStatus = create("RESOURCE_EXHAUSTED", 8)

    @JvmField val FAILED_PRECONDITION: GrpcStatus = create("FAILED_PRECONDITION", 9)

    @JvmField val ABORTED: GrpcStatus = create("ABORTED", 10)

    @JvmField val OUT_OF_RANGE: GrpcStatus = create("OUT_OF_RANGE", 11)

    @JvmField val UNIMPLEMENTED: GrpcStatus = create("UNIMPLEMENTED", 12)

    @JvmField val INTERNAL: GrpcStatus = create("INTERNAL", 13)

    @JvmField val UNAVAILABLE: GrpcStatus = create("UNAVAILABLE", 14)

    @JvmField val DATA_LOSS: GrpcStatus = create("DATA_LOSS", 15)

    @JvmField val UNAUTHENTICATED: GrpcStatus = create("UNAUTHENTICATED", 16)

    private fun create(name: String, status: Int): GrpcStatus {
      val result = GrpcStatus(name, status)
      INSTANCES[status] = result
      return result
    }

    @Synchronized
    fun get(status: Int): GrpcStatus {
      require(status >= 0)
      return INSTANCES.getOrPut(status) { GrpcStatus("STATUS_$status", status) }
    }
  }
}
