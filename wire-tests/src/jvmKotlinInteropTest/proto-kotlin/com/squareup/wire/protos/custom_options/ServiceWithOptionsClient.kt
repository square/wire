// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.custom_options.ServiceWithOptions in custom_options.proto
@file:Suppress("DEPRECATION")

package com.squareup.wire.protos.custom_options

import com.squareup.wire.GrpcCall
import com.squareup.wire.Service
import kotlin.Suppress

@ServiceOptionOneServiceOption(456)
public interface ServiceWithOptionsClient : Service {
  @MethodOptionOneMethodOption(789)
  public fun MethodWithOptions(): GrpcCall<FooBar, FooBar>
}
