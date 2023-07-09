/*
 * Copyright (C) 2019 Square, Inc.
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

/**
 * For gRPC actions the path is formatted as `/<service name>/<method name>`. The path of the proto
 * service below is `/squareup.helloworld.Greeter/SayHello`.
 *
 * ```
 * package squareup.helloworld;
 *
 * service Greeter {
 *   rpc SayHello (HelloRequest) returns (HelloReply) {}
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WireRpc(
  val path: String,
  val requestAdapter: String,
  val responseAdapter: String,
  val sourceFile: String = "",
)
