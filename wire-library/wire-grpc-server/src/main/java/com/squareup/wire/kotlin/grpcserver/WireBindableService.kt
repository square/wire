package com.squareup.wire.kotlin.grpcserver

import io.grpc.BindableService

/**
 * Wire specific interface for BindableService.
 *
 * This allows us to detect if the service is a Wire service at runtime, if needed.
 */
interface WireBindableService : BindableService
