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
 * A readable stream of messages.
 *
 * Typical implementations will receive messages recently transmitted from a peer, such as for
 * server-to-client or client-to-server networking. But this implementation is not limited to such
 * networking use cases and implementations may load messages from local storage or generate
 * messages on demand.
 *
 * Calls to [read] will block until a message becomes available. There is no mechanism to limit how
 * long a specific [read] will wait, though implementations may be configured to fail if they
 * consider a source to be unhealthy.
 *
 * Readers should take care to keep up with the stream of messages. A reader that takes an excessive
 * amount of time to process a message may cause their writer to back up and suffer queueing.
 *
 * Instances of this interface are not safe for concurrent use.
 */
expect interface MessageSource<out T : Any> {
  /**
   * Read the next length-prefixed message on the stream and return it. Returns null if there are
   * no further messages on this stream.
   *
   * @throws IOException if the next message cannot be read, or if the stream was abnormally
   *     terminated by its producer.
   */
  @Throws(IOException::class)
  fun read(): T?

  @Throws(IOException::class)
  fun close()
}
