package com.squareup.wire.kotlin.grpcserver

import com.squareup.wire.MessageSink
import io.grpc.stub.StreamObserver

// This is for adapting Google style grpc stubs to Wire style grpc stubs, in case we want to
// allow existing Misk gRPC implementations to be plugged in.
class MessageSinkAdapter<T:Any> constructor(private val responseObserver: StreamObserver<T>):
  MessageSink<T> {
  override fun cancel() {
    TODO("not implemented")
  }

  override fun close() {
    responseObserver.onCompleted()
  }

  override fun write(message: T) {
    responseObserver.onNext(message)
  }
}