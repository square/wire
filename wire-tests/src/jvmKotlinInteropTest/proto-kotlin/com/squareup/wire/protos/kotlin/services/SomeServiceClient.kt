// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.SomeService in service_kotlin.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.kotlin.services

import com.squareup.wire.GrpcCall
import com.squareup.wire.Service
import kotlin.Suppress

public interface SomeServiceClient : Service {
  public fun SomeMethod(): GrpcCall<SomeRequest, SomeResponse>
}
