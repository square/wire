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

import kotlin.Throws
import okio.IOException

/**
 * A writable stream of messages.
 *
 * Typical implementations will immediately encode messages and enqueue them for transmission, such
 * as for client-to-server or server-to-client networking. But this interface is not limited to 1-1
 * networking use cases and implementations may persist, broadcast, validate, or take any other
 * action with the messages.
 *
 * There is no flushing mechanism. Messages are flushed one-by-one as they are written. This
 * minimizes latency at a potential cost of throughput.
 *
 * On its own this offers no guarantees that messages are delivered. For example, a message may
 * accepted by [write] could be lost due to a network partition or crash. It is the caller's
 * responsibility to confirm delivery and to retransmit as necessary.
 *
 * It is possible for a writer to saturate the transmission channel, such as when a writer writes
 * faster than the corresponding reader can read. In such cases calls to [write] will block until
 * there is capacity in the outbound channel. You may use this as a basic backpressure mechanism.
 * You should ensure that such backpressure propagates to the originator of outbound messages.
 *
 * Instances of this interface are not safe for concurrent use.
 */
expect interface MessageSink<in T : Any> {
  /**
   * Encode [message] to bytes and enqueue the bytes for delivery, waiting if necessary until the
   * delivery channel has capacity for the encoded message.
   *
   * @throws IllegalStateException if this stream is closed.
   */
  @Throws(IOException::class)
  fun write(message: T)

  /**
   * Truncate this stream abnormally. This attempts to signal to readers of this data that it is
   * incomplete. Note that unlike some cancel methods this is not safe for concurrent use.
   *
   * A stream must be closed after it is canceled.
   *
   * @throws IllegalStateException if this stream is closed.
   */
  @Throws(IOException::class)
  fun cancel()

  /**
   * Terminate the stream and release its resources. If this has not been canceled this signals a
   * normal completion of the stream.
   */
  @Throws(IOException::class)
  fun close()
}
