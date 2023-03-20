package com.squareup.wire.kotlin.grpcserver

import io.grpc.MethodDescriptor

/**
 * Interface for generated method marshallers used in Google style grpc stubs.
 *
 * Adds a method for fetching the underlying class at run time to support
 * generating other marshallers (like JSON encoding) from an instance of
 * this class.
 */
interface WireMethodMarshaller<T> : MethodDescriptor.Marshaller<T> {
  fun marshalledClass(): Class<T>
}
