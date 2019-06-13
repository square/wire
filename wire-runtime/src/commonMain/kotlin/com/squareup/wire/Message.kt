package com.squareup.wire

import okio.BufferedSink
import okio.ByteString

/** A protocol buffer message. */
expect abstract class Message<M : Message<M, B>, B : Message.Builder<M, B>> {
  /** If non-zero, the hash code of this message. Accessed by generated code. */
  protected var hashCode: Int

  /**
   * Returns a byte string containing the proto encoding of this message's unknown fields. Returns
   * an empty byte string if this message has no unknown fields.
   */
  fun unknownFields(): ByteString

  /**
   * Returns a new builder initialized with the data in this message.
   */
  // TODO(egorand): Remove from common and not generate in Kotlin.
  abstract fun newBuilder(): B

  /** Returns this message with any unknown fields removed. */
  // TODO(egorand): Make abstract, KotlinGenerator should generate a call to copy(), while
  // JavaGenerator would still use Builder
  fun withoutUnknownFields(): M

  /** The [ProtoAdapter] for encoding and decoding messages of this type. */
  fun adapter(): ProtoAdapter<M>

  /** Encode this message and write it to `stream`. */
  fun encode(sink: BufferedSink)

  /** Encode this message as a `byte[]`. */
  fun encode(): ByteArray

  /**
   * Superclass for protocol buffer message builders.
   */
  abstract class Builder<M : Message<M, B>, B : Builder<M, B>>
}
