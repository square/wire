/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.kotlin.grpcserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * This is an adapter class to convert Wire generated Channel based routines to
 * flow based functions compatible with io.grpc:protoc-gen-grpc-kotlin.
 */
object FlowAdapter {

  fun <I : Any, O : Any> serverStream(
    context: CoroutineContext,
    request: I,
    f: suspend (I, SendChannel<O>) -> Unit
  ): Flow<O> {
    val sendChannel = Channel<O>()

    CoroutineScope(context).launch { f(request, sendChannel) }
    return sendChannel.consumeAsFlow()
  }

  suspend fun <I : Any, O : Any> clientStream(
    context: CoroutineContext,
    request: Flow<I>,
    f: suspend (ReceiveChannel<I>) -> O
  ): O {
    val receiveChannel = Channel<I>()

    CoroutineScope(context).launch {
      request
        .onCompletion { receiveChannel.close() }
        .collect { receiveChannel.send(it) }
    }
    return f(receiveChannel)
  }

  fun <I : Any, O : Any> bidiStream(
    context: CoroutineContext,
    request: Flow<I>,
    f: suspend (ReceiveChannel<I>, SendChannel<O>) -> Unit
  ): Flow<O> {
    val sendChannel = Channel<O>()
    val receiveChannel = Channel<I>()

    CoroutineScope(context).launch {
      request
        .onCompletion { receiveChannel.close() }
        .collect { receiveChannel.send(it) }
    }
    CoroutineScope(context).launch { f(receiveChannel, sendChannel) }

    return sendChannel.consumeAsFlow()
  }
}
