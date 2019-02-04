/*
 * Copyright 2019 Square Inc.
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
package routeguide

import com.squareup.wire.Service
import com.squareup.wire.WireRpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext

// TODO(oldergod) codegen that interface
interface RouteGuide : Service {
  @WireRpc(
      path = "/routeguide.RouteGuide/GetFeature",
      requestAdapter = "routeguide.Point#ADAPTER",
      responseAdapter = "routeguide.Feature#ADAPTER"
  )
  suspend fun GetFeature(context: CoroutineContext, request: Point): Feature

  @WireRpc(
      path = "/routeguide.RouteGuide/ListFeatures",
      requestAdapter = "routeguide.Rectangle#ADAPTER",
      responseAdapter = "routeguide.Feature#ADAPTER"
  )
  fun ListFeatures(context: CoroutineContext, request: Rectangle): ReceiveChannel<Feature>

  @WireRpc(
      path = "/routeguide.RouteGuide/RecordRoute",
      requestAdapter = "routeguide.Point#ADAPTER",
      responseAdapter = "routeguide.RouteSummary#ADAPTER"
  )
  fun RecordRoute(context: CoroutineContext): Pair<SendChannel<Point>, Deferred<RouteSummary>>

  @WireRpc(
      path = "/routeguide.RouteGuide/RouteChat",
      requestAdapter = "routeguide.RouteNote#ADAPTER",
      responseAdapter = "routeguide.RouteNote#ADAPTER"
  )
  fun RouteChat(context: CoroutineContext): Pair<SendChannel<RouteNote>, ReceiveChannel<RouteNote>>
}
